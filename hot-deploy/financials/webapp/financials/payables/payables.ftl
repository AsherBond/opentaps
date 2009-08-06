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

<div class="tabletext" style="margin-bottom: 30px;">

<p class="tableheadtext">${uiLabelMap.AccountingInvoices}
<ul class="bulletList">
  <#if parameters.hasFinancialsAPInvoiceCreatePermission>
    <li class="tabletext"><a href="<@ofbizUrl>createInvoiceForm?invoiceTypeId=PURCHASE_INVOICE</@ofbizUrl>">${uiLabelMap.FinancialsCreatePurchaseInvoice}</a></li>
    <li class="tabletext"><a href="<@ofbizUrl>createInvoiceForm?invoiceTypeId=CUST_RTN_INVOICE</@ofbizUrl>">${uiLabelMap.FinancialsCreateCustomerReturnInvoice}</a></li>
  </#if>
  <li class="tabletext"><a href="<@ofbizUrl>findPurchaseInvoices</@ofbizUrl>">${uiLabelMap.FinancialsFindPurchaseInvoices}</a></li>
  <li class="tabletext"><a href="<@ofbizUrl>findCustomerReturnInvoices</@ofbizUrl>">${uiLabelMap.FinancialsFindCustomerReturnInvoices}</a></li>
</ul>
</p>

<p class="tableheadtext">${uiLabelMap.AccountingPayment}
<ul class="bulletList">
<#if parameters.hasFinancialsAPPaymentCreatePermission>
  <li class="tabletext">
    <form action="<@ofbizUrl>editPayment</@ofbizUrl>" method="get" name="makePaymentForm">
        <a href="javascript:document.makePaymentForm.submit()">${uiLabelMap.FinancialsPayablesPaymentToPartyId}</a>
        <@inputAutoCompleteParty name="partyId" id="makePaymentFormPartyId" />
        <@inputHidden name="paymentTypeId" value="DISBURSEMENT" />
    </form>
  </li>
</#if>
<li class="tabletext"><a href="<@ofbizUrl>findPayment?findPaymentTypeId=DISBURSEMENT</@ofbizUrl>">${uiLabelMap.FinancialsFindPayment}</a></li>
<#if parameters.hasFinancialsAPPaymentCreatePermission>
  <li class="tabletext"><a href="<@ofbizUrl>checkRun</@ofbizUrl>">${uiLabelMap.FinancialsCheckRun}</a></li>
  <li class="tabletext"><a href="<@ofbizUrl>listChecksToPrint</@ofbizUrl>">${uiLabelMap.FinancialsPrintChecks}</a></li>
</#if>
<#if parameters.hasFinancialsAPPaymentUpdatePermission>
  <li class="tabletext"><a href="<@ofbizUrl>confirmSentPaymentsForm</@ofbizUrl>">${uiLabelMap.FinancialsConfirmSentPayments}</a></li>
</#if>
</ul>
</p>
<p class="tableheadtext">${uiLabelMap.FinancialsCommissions}
<ul class="bulletList">
  <#if parameters.hasCreateAgreementPermission>
  <li class="tabletext"><@displayLink href="createCommissionAgreement" text="${uiLabelMap.FinancialsCreateCommissionAgreement}" class=""/></li>
  </#if>
  <#if parameters.hasViewAgreementPermission>
  <li class="tabletext"><@displayLink href="findCommissionAgreements" text="${uiLabelMap.FinancialsFindCommissionAgreements}" class=""/></li>
  </#if>
  <#if parameters.hasFinancialsAPInvoiceCreatePermission>
    <li class="tabletext"><a href="<@ofbizUrl>createInvoiceForm?invoiceTypeId=COMMISSION_INVOICE</@ofbizUrl>">${uiLabelMap.FinancialsCreateCommissionInvoice}</a></li>
  </#if>
  <li class="tabletext"><a href="<@ofbizUrl>findCommissionInvoices</@ofbizUrl>">${uiLabelMap.FinancialsFindCommissionInvoices}</a></li>
<li class="tabletext"><a href="<@ofbizUrl>commissionBalancesReport</@ofbizUrl>">${uiLabelMap.FinancialsCommissionBalancesReport}</a>
<li class="tabletext"><a href="<@ofbizUrl>commissionsStatement</@ofbizUrl>">${uiLabelMap.FinancialsCommissionsStatement}</a>
</ul>
</p>
<p class="tableheadtext">${uiLabelMap.AccountingReports}
<ul class="bulletList">
<li class="tabletext"><a href="<@ofbizUrl>payablesBalancesReport</@ofbizUrl>">${uiLabelMap.FinancialsPayablesBalancesReport}</a>
<li class="tabletext"><a href="<@ofbizUrl>vendorStatement</@ofbizUrl>">${uiLabelMap.FinancialsVendorStatement}</a>
<li class="tabletext"><a href="<@ofbizUrl>payablesAgingReport</@ofbizUrl>">${uiLabelMap.FinancialsPayablesAgingReport}</a>
<li class="tabletext"><a href="<@ofbizUrl>AverageDSOReportPayables</@ofbizUrl>">${uiLabelMap.FinancialsAverageDSOReportPayables}</a>
<li class="tabletext"><a href="<@ofbizUrl>CommissionReport</@ofbizUrl>">${uiLabelMap.FinancialsCommissionReport}</a>
</ul>
</p>
</div>
