<#--
 * Copyright (c) Open Source Strategies, Inc.
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

<table class="listTable" >
  <tr class="boxtop">
    <td><span class="boxhead">${uiLabelMap.AccountingInvoice}</span></td>
    <td><span class="boxhead">${uiLabelMap.FinancialsPaymentRef}</span></td>
    <td><span class="boxhead">${uiLabelMap.CommonDescription}</span></td>
    <td><span class="boxhead">${uiLabelMap.AccountingInvoiceDate}</span></td>
    <td><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
    <td><span class="boxhead">${uiLabelMap.AccountingAmountApplied}</span></td>
    <td><span class="boxhead">${uiLabelMap.CommonNote}</span></td>
    <td><span class="boxhead"></span></td>
    <td><span class="boxhead"></span></td>
  </tr>
  <@form name="removePaymentApplicationForm" url="removePaymentApplication" paymentApplicationId="" paymentId="" />
  <#list paymentApplicationsList as row>
    <form name="listPaymentApplications_${row_index}" action="updatePaymentApplication" method="POST" class="basic-form">
      <tr class="viewManyTR2">
        <@inputHidden name="paymentApplicationId" value=row.paymentApplicationId />
        <@inputHidden name="paymentId" value=row.paymentId?if_exists />
        <@inputHidden name="invoiceId" value=row.invoiceId?if_exists />
        <@inputHidden name="invoiceRefNum" value=row.invoiceRefNum?if_exists />
        <@inputHidden name="description" value=row.description?if_exists />
        <@inputHidden name="invoiceDate" value=row.invoiceDate?if_exists />
        <@inputHidden name="dummy" value=row.outstandingAmount?if_exists />
        <@displayLinkCell text=row.invoiceId href="viewInvoice?invoiceId=${row.invoiceId}"/>
        <@displayCell text=row.invoiceRefNum/>
        <@displayCell text=row.description/>
        <@displayDateCell date=row.invoiceDate/>
        <@displayCurrencyCell amount=row.outstandingAmount currencyUomId=row.currencyUomId />
        <@inputTextCell name="amountApplied" ignoreParameters=true default=row.amountApplied/>
        <@inputTextCell name="note" ignoreParameters=true default=row.note/>
        <@displayLinkCell class="buttontext" href="javascript:document.listPaymentApplications_${row_index}.submit();" text=uiLabelMap.CommonUpdate/>
        <td>
          <@submitFormLink form="removePaymentApplicationForm" class="buttontext" text=uiLabelMap.CommonRemove paymentApplicationId=row.paymentApplicationId paymentId=row.paymentId/>
        </td>
      </tr>
      <#if tagTypes?has_content && allocatePaymentTagsToApplications>
        <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="2" entity=row!/>
      </#if>
    </form>
  </#list>
</table>
