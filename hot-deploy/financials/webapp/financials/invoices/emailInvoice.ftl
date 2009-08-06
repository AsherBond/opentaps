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
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if invoice?has_content>

<#if invoice.invoiceTypeId == "SALES_INVOICE">
  <#assign title = uiLabelMap.FinancialsSalesInvoice />
<#elseif invoice.invoiceTypeId == "PURCHASE_INVOICE">
  <#assign title = uiLabelMap.FinancialsPurchaseInvoice />
<#elseif invoice.invoiceTypeId == "CUST_RTN_INVOICE">
  <#assign title = uiLabelMap.FinancialsCustomerReturnInvoice />
<#elseif invoice.invoiceTypeId == "COMMISSION_INVOICE">
  <#assign title = uiLabelMap.FinancialsCommissionInvoice />
<#elseif invoice.invoiceTypeId == "INTEREST_INVOICE">
  <#assign title = uiLabelMap.FinancialsInterestInvoice />
</#if>


<div class="screenlet">
  <div class="screenlet-header">
    <div class="boxhead">${title} ${uiLabelMap.OrderNbr}<a href="<@ofbizUrl>viewInvoice?invoiceId=${invoice.invoiceId}</@ofbizUrl>" class="buttontext">${invoice.invoiceId}</a></div>
  </div>

  <table width="100%">
    <form method="post" action="<@ofbizUrl>sendInvoiceEmail</@ofbizUrl>" name="emailInvoice">
      <@inputHidden name="invoiceId" value="${invoice.invoiceId}"/>
      <#if userLoginEmails.size() != 0>
        <@inputSelectRow name="sendFrom" title=uiLabelMap.CommonFrom list=userLoginEmails key="infoString" displayField="infoString" />
      <#else>
        <@inputTextRow name="sendFrom" title=uiLabelMap.CommonFrom size=60 />
      </#if>
      <@inputTextRow name="sendTo" title=uiLabelMap.CommonTo size=60 default="${recipientEmailAddress?if_exists}"/>
      <@inputTextRow name="sendCc" title="CC" size=60 />
      <@inputTextRow name="subject" title=uiLabelMap.PartySubject default="${uiLabelMap.AccountingInvoice} #${invoice.invoiceId}" size=60 />
      <@inputTextareaRow name="bodyText" title=uiLabelMap.CommonMessage />
      <@displayLinkRow href="invoice.pdf?invoiceId=${invoice.invoiceId}" title="PDF" text="${uiLabelMap.AccountingInvoice} ${invoice.invoiceId}.pdf"/>
      <@inputSubmitRow title=uiLabelMap.CommonSend />
  </form>
  </table>
</div>

<#else> <#-- if invoice has no content -->
  ${uiLabelMap.FinancialsInvoiceNotFound}
</#if>
