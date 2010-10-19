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
 *  
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<table width="100%">
    <tr>
       <td class="tableheadtext">${uiLabelMap.CommonDate}</td>
       <td class="tableheadtext">${uiLabelMap.FinancialsTransaction}</td>
       <td class="tableheadtext" align="right">${uiLabelMap.AccountingAmount}</td>
       <td class="tableheadtext" align="right">${uiLabelMap.AccountingBillingAvailableBalance}</td>
    </tr>
    
    
<#-- this block basically implements the business logic that a PaymentApplication with paymentId and billingAccountId alone decreases the balance on the account,
whereas a PaymentApplication with paymentId, billingAccountId, and invoiceId increases the net balance.  available balance = accountLimit - net balance.
Of course, we're not considering cases where accountLimit may have different in the past -->
    
<#assign accountBalance = 0.0>
<#list billAcctPaymentAndApplications as paymentAppl> 

   <#-- we need to skip customer return and CANCELLED invoices -->
   <#assign skipPayAppl = false>
   <#if (paymentAppl.invoiceId?exists)>
      <#assign invoice = paymentAppl.getRelatedOne("PaymentApplication").getRelatedOne("Invoice")>
      <#if (invoice.invoiceTypeId == "CUST_RTN_INVOICE") || (invoice.statusId=="INVOICE_CANCELLED")>
          <#assign skipPayAppl = true>
      </#if>
   </#if>

   <#if !(paymentAppl.invoiceId?exists) && (paymentAppl.paymentId?exists)>
      <#assign accountBalance = accountBalance - paymentAppl.amountApplied>
   <#elseif (paymentAppl.invoiceId != null) && (paymentAppl.paymentId != null) && !(skipPayAppl)>
      <#assign accountBalance = accountBalance + paymentAppl.amountApplied>   
   </#if>
   
   <#if !skipPayAppl>
   <tr>
       <td class="tabletext">${getLocalizedDate(paymentAppl.effectiveDate)}</td>

       <td class="tabletext">

       <#if paymentAppl.invoiceId?has_content>
       ${invoice.getRelatedOneCache("InvoiceType").description?default("")} #<a href="<@ofbizUrl>viewInvoice?invoiceId=${paymentAppl.invoiceId}</@ofbizUrl>">${paymentAppl.invoiceId}</a>
       <#elseif paymentAppl.paymentId?has_content>
       ${paymentAppl.getRelatedOneCache("PaymentType").description?default("")} #<a href="<@ofbizUrl>viewPayment?paymentId=${paymentAppl.paymentId}</@ofbizUrl>">${paymentAppl.paymentId}</a>
       </#if>
       </td>

       <td class="tabletextright"><@ofbizCurrency amount=paymentAppl.amountApplied isoCode=paymentAppl.currencyUomId/></td>
       <#assign availableBalance = billingAccount.accountLimit?default(0) - accountBalance>
       <td class="tabletextright"><@ofbizCurrency amount=availableBalance isoCode=paymentAppl.currencyUomId/></td>
   </tr>
   <#if paymentAppl.comments?has_content>
   <tr>
       <td class="tabletext">&nbsp;</td>
       <td class="tabletext" colspan="3"><i>${paymentAppl.comments}</i></td>
   </tr>
   </#if>
   </#if>
</#list>
</table>

