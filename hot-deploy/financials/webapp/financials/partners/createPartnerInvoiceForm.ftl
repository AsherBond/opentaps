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

<div class="screenlet">

  <div class="screenlet-header">
    <div class="boxhead">${uiLabelMap.FinancialsCreatePartnerInvoice}</div>
  </div>
  <div class="screenlet-body">
    <table border="0" cellpadding="2" cellspacing="0" width="100%">
      <form method="post" action="<@ofbizUrl>createPartnerInvoice</@ofbizUrl>" name="createPartnerInvoiceForm">
        <@inputHidden name="invoiceTypeId" value="PARTNER_INVOICE"/>
        <@inputHidden name="statusId" value="INVOICE_IN_PROCESS"/>
        <@inputSelectRow name="partyIdFrom" title=uiLabelMap.OpentapsPartner list=partners key="partyId" ; partner >
          ${partner.firstName?if_exists} ${partner.lastName?if_exists} ${partner.groupName?if_exists} (${partner.partyId})
        </@inputSelectRow>
        <@inputLookupRow name="partyId" title=uiLabelMap.CommonCustomer lookup="LookupPartyName" form="createPartnerInvoiceForm" />
        <@inputCurrencySelectRow name="currencyUomId" title=uiLabelMap.CommonCurrency defaultCurrencyUomId=parameters.orgCurrencyUomId />
        <@inputDateTimeRow name="invoiceDate" title=uiLabelMap.AccountingInvoiceDate form="createPartnerInvoiceForm" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
        <@inputDateTimeRow name="dueDate" title=uiLabelMap.AccountingDueDate form="createPartnerInvoiceForm" />
        <@inputTextRow name="referenceNumber" title=uiLabelMap.FinancialsReferenceNumber size=60 />
        <@inputTextRow name="description" title=uiLabelMap.CommonDescription size=60 />
        <@inputTextRow name="invoiceMessage" title=uiLabelMap.CommonMessage size=60 />
        <@inputSubmitRow title=uiLabelMap.CommonCreate />
      </form>
    </table>
  </div>

</div>
