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
  </tr>
  <#list paymentApplicationsList as row>
    <tr class="viewManyTR2">
      <@displayLinkCell text=row.invoiceId! href="viewInvoice?invoiceId=${row.invoiceId?default(\"\")}"/>
      <@displayCell text=row.invoiceRefNum/>
      <@displayCell text=row.description/>
      <@displayDateCell date=row.invoiceDate/>
      <@displayCurrencyCell amount=row.outstandingAmount currencyUomId=row.currencyUomId />
      <@displayCurrencyCell amount=row.amountApplied currencyUomId=row.currencyUomId/>
      <@displayCell text=row.note/>
    </tr>
    <#if tagTypes?has_content && allocatePaymentTagsToApplications>
      <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="2" entity=row! readonly="true"/>
    </#if>            
  </#list>
</table>
