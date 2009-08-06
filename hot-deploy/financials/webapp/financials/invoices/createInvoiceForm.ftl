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

<#if invoiceTypeId == "SALES_INVOICE">
  <#assign createInvoiceTitle = uiLabelMap.FinancialsCreateSalesInvoice />
  <#assign whichPartyTitle = uiLabelMap.AccountingToParty />
<#elseif invoiceTypeId == "PURCHASE_INVOICE">
  <#assign createInvoiceTitle = uiLabelMap.FinancialsCreatePurchaseInvoice />
  <#assign whichPartyTitle = uiLabelMap.AccountingFromParty />
<#elseif invoiceTypeId == "CUST_RTN_INVOICE">
  <#assign createInvoiceTitle = uiLabelMap.FinancialsCreateCustomerReturnInvoice />
  <#assign whichPartyTitle = uiLabelMap.AccountingFromParty />
<#elseif invoiceTypeId == "COMMISSION_INVOICE">
  <#assign createInvoiceTitle = uiLabelMap.FinancialsCreateCommissionInvoice />
  <#assign whichPartyTitle = uiLabelMap.AccountingFromParty />
</#if>
<#if isDisbursement>
  <#assign whichPartyId = "partyIdFrom">
  <#assign orgPartyId = "partyId">
<#else>
  <#assign whichPartyId = "partyId">
  <#assign orgPartyId = "partyIdFrom">
</#if>


<div class="screenlet">

  <div class="screenlet-header">
    <div class="boxhead">${createInvoiceTitle}</div>
  </div>
  <div class="screenlet-body">
    <table border="0" cellpadding="2" cellspacing="0" width="100%">
      <form method="post" action="<@ofbizUrl>createInvoice</@ofbizUrl>" name="createInvoiceForm">
        <@inputHidden name="invoiceTypeId" value="${invoiceTypeId}"/>
        <@inputHidden name="statusId" value="INVOICE_IN_PROCESS"/>
        <@inputHidden name="${orgPartyId}" value="${parameters.organizationPartyId}" />
        <#if parameters.oldRefNum?exists><@inputHidden name="oldRefNum" value=parameters.oldRefNum /></#if>
        <@inputAutoCompletePartyRow title=whichPartyTitle name=whichPartyId id="autoCompleteWhichPartyId" size="20"/>
        <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency defaultCurrencyUomId=parameters.orgCurrencyUomId />
        <@inputDateTimeRow name="invoiceDate" title=uiLabelMap.AccountingInvoiceDate form="createInvoiceForm" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
        <@inputDateTimeRow name="dueDate" title=uiLabelMap.AccountingDueDate form="createInvoiceForm" />
        <@inputTextRow name="referenceNumber" title=uiLabelMap.FinancialsReferenceNumber size=60 />
        <@inputTextRow name="description" title=uiLabelMap.CommonDescription size=60 />
        <@inputTextRow name="invoiceMessage" title=uiLabelMap.CommonMessage size=60 />
        <@inputForceCompleteRow title=uiLabelMap.CommonCreate forceTitle=uiLabelMap.OpentapsForceCreate form="createInvoiceForm" /></td>
      </form>
    </table>
  </div>

</div>
