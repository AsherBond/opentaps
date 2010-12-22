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

<#macro generateStatementOld transactions beginningBalance=0 showBalances=true>
<#if transactions.size() != 0>
  <tr><td colspan="4">&nbsp;</td></tr>

  <tr>
    <td class="tableheadtext" width="20%">${uiLabelMap.CommonDate}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsTransaction}</td>
    <td class="tableheadtext" align="right">${uiLabelMap.AccountingAmount}</td>
    <td class="tableheadtext" align="right">${uiLabelMap.FinancialsStatementsBalance}</td>
  </tr>
  
  <#if showBalances>
  <tr>
    <td>&nbsp;</td>
    <td class="tabletext" colspan="2">${uiLabelMap.FinancialsStatementsBeginningBalance}</td>
    <td colspan="2" class="tabletext" align="right"><@ofbizCurrency amount=beginningBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  </#if>

  <#assign runningBalance = beginningBalance?default(0)/>
  <#list transactions as transaction>
    <#-- the transaction amount is based on Debit/Credit flag setting and the type of accounts (receivables vs. payables) -->
    <#assign transactionAmount = 0> 
    <#if isReceivable>
      <#if transaction.get("debitCreditFlag").equals("C")>
         <#assign transactionAmount = 0 - transaction.amount>
      <#elseif transaction.get("debitCreditFlag").equals("D")>
         <#assign transactionAmount = transaction.amount>
      </#if>
    <#elseif isPayable>
      <#if transaction.get("debitCreditFlag").equals("D")>
         <#assign transactionAmount = 0 - transaction.amount>
      <#elseif transaction.get("debitCreditFlag").equals("C")>
         <#assign transactionAmount = transaction.amount>
      </#if>
    </#if>
    
  <tr>
    <@displayDateCell date=transaction.transactionDate />
    <td class="tabletext">
    <#-- maybe at some point show other transaction types -->
    <#if transaction.acctgTransTypeId="REVERSE">${uiLabelMap.FinancialsReversalOf}</#if>
    <#if transaction.acctgTransTypeId="WRITEOFF">${uiLabelMap.FinancialsWriteoffOf}</#if>
  
    <#-- this can get a bit dangerous doing all these getRelated's but it should be OK because the relationships should (better!) be there --> 
    <#if transaction.invoiceId?exists>
      <#-- display invoice type and invoice number with links -->
      ${transaction.getRelatedOne("Invoice").getRelatedOneCache("InvoiceType").description?default("")} #<a href="<@ofbizUrl>viewInvoice?invoiceId=${transaction.invoiceId}</@ofbizUrl>">${transaction.invoiceId}</a>
       (<a href="<@ofbizUrl>invoice.pdf?invoiceId=${transaction.invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>" class="linktext">PDF</a>)
       
    <#elseif transaction.paymentId?exists>
      <#-- display payment type and payment number -->
      ${transaction.getRelatedOne("Payment").getRelatedOneCache("PaymentType").description?default("")} #<a href="<@ofbizUrl>/viewPayment?paymentId=${transaction.paymentId}</@ofbizUrl>">${transaction.paymentId}</a>
    
    <#else>
      <#-- not an invoice or payment?  display type of accounting transaction and details from transaction description -->
      ${transaction.getRelatedOneCache("AcctgTransType").description?default("")}: ${transaction.transDescription?default("")}
    </#if>
    
    <#-- view the actual transaction -->
    <#if parameters.hasFinancialsTransactionPermission>
    (<a href="<@ofbizUrl>viewAcctgTrans?acctgTransId=${transaction.acctgTransId}</@ofbizUrl>" class="linktext">${uiLabelMap.FinancialsTransaction}</a>)
    </#if>
    </td> 
      <td class="tabletext" align="right"><@ofbizCurrency amount=transactionAmount isoCode=orgCurrencyUomId/></td>
      <#assign runningBalance = runningBalance + transactionAmount>
      <td class="tabletext" align="right"><@ofbizCurrency amount=runningBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  </#list>

  <#if showBalances>
  <tr>
    <td>&nbsp;</td>
    <td class="tabletext" colspan="2">${uiLabelMap.FinancialsStatementsEndingBalance}</td>
    <td colspan="2" class="tabletext" align="right"><@ofbizCurrency amount=runningBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  </#if>

</#if>
</#macro>

<#-- refactored macro -->
<#macro generateStatement transactions beginningBalance=0 showBalances=true >
<#if transactions.size() != 0>
  <tr><td colspan="4">&nbsp;</td></tr>

  <tr>
    <td class="tableheadtext" width="20%">${uiLabelMap.CommonDate}</td>
    <td class="tableheadtext">${uiLabelMap.FinancialsTransaction}</td>
    <td class="tableheadtext" align="right">${uiLabelMap.AccountingAmount}</td>
    <td class="tableheadtext" align="right">${uiLabelMap.FinancialsStatementsBalance}</td>
  </tr>
  
  <#if showBalances>
  <tr>
    <td>&nbsp;</td>
    <td class="tabletext" colspan="2">${uiLabelMap.FinancialsStatementsBeginningBalance}</td>
    <td colspan="2" class="tabletext" align="right"><@ofbizCurrency amount=beginningBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  </#if>

  <#assign endingBalance=0 />

  <#list transactions as transaction>    
  <tr>
    <@displayDateCell date=transaction.transactionDate />
    <td class="tabletext">
    <#-- maybe at some point show other transaction types -->
    <#if transaction.acctgTransTypeId="REVERSE">${uiLabelMap.FinancialsReversalOf}</#if>
    <#if transaction.acctgTransTypeId="WRITEOFF">${uiLabelMap.FinancialsWriteoffOf}</#if>
  
    <#if transaction.invoiceId?exists>
      <#-- display invoice type and invoice number with links -->
      ${transaction.invoiceType?default("")} #<a href="<@ofbizUrl>viewInvoice?invoiceId=${transaction.invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>">${transaction.invoiceId}</a>
       (<a href="<@ofbizUrl>invoice.pdf?invoiceId=${transaction.invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>" class="linktext">PDF</a>)
    <#elseif transaction.paymentId?exists>
      <#-- display payment type and payment number -->
      ${transaction.paymentType?default("")} #<a href="<@ofbizUrl>/viewPayment?paymentId=${transaction.paymentId}</@ofbizUrl>">${transaction.paymentId}</a>
    <#else>
      <#-- not an invoice or payment?  display type of accounting transaction and details from transaction description -->
      ${transaction.transType?default("")}: ${transaction.transDescription?default("")}
    </#if>
    
    <#-- view the actual transaction -->
    <#if parameters.hasFinancialsTransactionPermission>
    (<a href="<@ofbizUrl>viewAcctgTrans?acctgTransId=${transaction.acctgTransId}</@ofbizUrl>" class="linktext">${uiLabelMap.FinancialsTransaction}</a>)
    </#if>
    </td> 
      <td class="tabletext" align="right"><@ofbizCurrency amount=transaction.amount isoCode=orgCurrencyUomId/></td>
      <td class="tabletext" align="right"><@ofbizCurrency amount=transaction.runningBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  <#assign endingBalance=transaction.runningBalance />
  </#list>

  <#if showBalances>
  <tr>
    <td>&nbsp;</td>
    <td class="tabletext" colspan="2">${uiLabelMap.FinancialsStatementsEndingBalance}</td>
    <td colspan="2" class="tabletext" align="right"><@ofbizCurrency amount=endingBalance isoCode=orgCurrencyUomId/></td>
  </tr>
  </#if>

</#if>
</#macro>
