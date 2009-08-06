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
<div class="form">

<div class="tabletext" style="margin-bottom: 30px;">

  <p class="tableheadtext">${uiLabelMap.AccountingInvoices}</p>
  <ul class="bulletList">
    <#if financialsSecurity.hasCreatePartnerInvoicePermission()>
      <li class="tabletext"><a href="<@ofbizUrl>createPartnerInvoiceForm</@ofbizUrl>">${uiLabelMap.FinancialsCreatePartnerInvoice}</a></li>
    </#if>
    <#if financialsSecurity.hasViewPartnerInvoicePermission()>
      <li class="tabletext"><a href="<@ofbizUrl>findPartnerInvoices</@ofbizUrl>">${uiLabelMap.FinancialsFindPartnerInvoices}</a></li>
    </#if>
    <#if financialsSecurity.hasCreatePartnerInvoicePermission()>
      <li class="tabletext"><a href="<@ofbizUrl>invoicePartnersForm</@ofbizUrl>">${uiLabelMap.FinancialsInvoicePartners}</a></li>
    </#if>
  </ul>

  <p class="tableheadtext">${uiLabelMap.AccountingAgreements}</p>
  <ul class="bulletList">
    <#if financialsSecurity.hasCreatePartnerAgreementPermission()>
      <li class="tabletext"><a href="<@ofbizUrl>createPartnerAgreement</@ofbizUrl>">${uiLabelMap.FinancialsCreatePartnerAgreement}</a></li>
    </#if>
    <#if financialsSecurity.hasViewPartnerAgreementPermission()>
      <li class="tabletext"><a href="<@ofbizUrl>findPartnerAgreements</@ofbizUrl>">${uiLabelMap.FinancialsFindPartnerAgreements}</a></li>
    </#if>
  </ul>

</div>
