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
