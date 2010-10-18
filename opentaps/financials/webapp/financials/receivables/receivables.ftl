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

<div class="tabletext" style="margin-bottom: 30px;">

  <p class="tableheadtext">${uiLabelMap.AccountingInvoices}
    <ul class="bulletList">
      <#if parameters.hasFinancialsARInvoiceCreatePermission><li class="tabletext"><a href="<@ofbizUrl>createInvoiceForm?invoiceTypeId=SALES_INVOICE</@ofbizUrl>">${uiLabelMap.FinancialsCreateSalesInvoice}</a></li></#if>
      <li class="tabletext"><a href="<@ofbizUrl>findSalesInvoices</@ofbizUrl>">${uiLabelMap.FinancialsFindSalesInvoices}</a></li>
      <li class="tabletext">
        <form action="<@ofbizUrl>invoicePerformedOrderItems</@ofbizUrl>" method="post" name="invoicePerformedOrderItems">
          <a href="javascript:document.invoicePerformedOrderItems.submit()">${uiLabelMap.FinancialsInvoicePerformedOrderItemsForOrder}</a>
          <@inputLookup name="orderId" lookup="LookupSalesOrders?statusId=ORDER_APPROVED" form="invoicePerformedOrderItems"/>
        </form>
      </li>
    </ul>
  </p>

  <p class="tableheadtext">${uiLabelMap.AccountingAgreements}
    <ul class="bulletList">
      <#if (security.hasEntityPermission("FINANCIALS", "_SAGR_UPDT", session) && security.hasEntityPermission("ACCOUNTING", "_CREATE", session))>
        <li class="tabletext"><a href="<@ofbizUrl>createSalesAgreement</@ofbizUrl>">${uiLabelMap.FinancialsCreateCustomerAgreement}</a></li>
      </#if>
      <#if (security.hasEntityPermission("FINANCIALS", "_SAGR_VIEW", session))>
        <li class="tabletext"><a href="<@ofbizUrl>findSalesAgreements</@ofbizUrl>">${uiLabelMap.FinancialsFindCustomerAgreements}</a></li>
      </#if>
    </ul>
  </p>

  <p class="tableheadtext">${uiLabelMap.AccountingPayment}
    <ul class="bulletList">
      <#if parameters.hasFinancialsARPaymentCreatePermission>
        <li class="tabletext">
          <form action="<@ofbizUrl>editPayment</@ofbizUrl>" method="get" name="receivePaymentForm">
            <a href="javascript:document.receivePaymentForm.submit()">${uiLabelMap.FinancialsReceivablesPaymentFromPartyId}</a>
            <@inputAutoCompleteParty name="partyId" id="receivePaymentFormPartyId" />
            <@inputHidden name="paymentTypeId" value="RECEIPT" />
          </form>
        </li>
      </#if>
      <li class="tabletext"><a href="<@ofbizUrl>findPayment?findPaymentTypeId=RECEIPT</@ofbizUrl>">${uiLabelMap.FinancialsFindPayment}</a>
      <#if parameters.hasFinancialsARPaymentCreatePermission && parameters.hasFinancialsARInvoiceCreatePermission>
        <li class="tabletext"><a href="<@ofbizUrl>CODReceipt</@ofbizUrl>">${uiLabelMap.FinancialsReceiveCODStatement}</a>
      </#if>
      <#if (security.hasEntityPermission("OPENTAPS", "_CSHDRWR", session))>
        <li><a href="<@ofbizUrl>manageCashDrawers</@ofbizUrl>">${uiLabelMap.OpentapsCashDrawerManage}</a></li>
      </#if>
      <li><a href="<@ofbizUrl>manageLockboxBatches</@ofbizUrl>">${uiLabelMap.FinancialsManageLockboxBatches}</a></li>
    </ul>
  </p>

  <p class="tableheadtext">${uiLabelMap.FinancialsCustomerBillingAccounts}
    <ul class="bulletList">
      <li class="tabletext"><a href="<@ofbizUrl>findCustomerBillAcct</@ofbizUrl>">${uiLabelMap.FinancialsFindCustomerBillingAccount}</a>
    </ul>
  </p>

  <p class="tableheadtext">${uiLabelMap.FinancialsFinanceCharges}
    <ul class="bulletList">
      <li class="tabletext"><a href="<@ofbizUrl>findFinanceCharges</@ofbizUrl>">${uiLabelMap.FinancialsFindFinanceCharges}</a></li>
      <#if parameters.hasFinancialsARInvoiceCreatePermission>
        <li class="tabletext"><a href="<@ofbizUrl>assessFinanceChargesForm</@ofbizUrl>">${uiLabelMap.FinancialsAssessFinanceCharges}</a></li>
      </#if>
    </ul>
  </p>

  <p class="tableheadtext">${uiLabelMap.AccountingReports}
    <ul class="bulletList">
      <li class="tabletext"><a href="<@ofbizUrl>receivablesBalancesReport</@ofbizUrl>">${uiLabelMap.FinancialsReceivablesBalancesReport}</a></li>
      <li class="tabletext"><a href="<@ofbizUrl>customerStatement</@ofbizUrl>">${uiLabelMap.FinancialsCustomerStatement}</a></li>
      <li class="tabletext"><a href="<@ofbizUrl>receivablesAgingReport</@ofbizUrl>">${uiLabelMap.FinancialsReceivablesAgingReport}</a></li>
      <li class="tabletext"><a href="<@ofbizUrl>AverageDSOReportReceivables</@ofbizUrl>">${uiLabelMap.FinancialsAverageDSOReportReceivables}</a></li>
      <li class="tabletext"><a href="<@ofbizUrl>CreditCardReport</@ofbizUrl>">${uiLabelMap.FinancialsCreditCardReport}</a></li>  
      <li class="tabletext"><a href="<@ofbizUrl>PaymentReceiptsDetail</@ofbizUrl>">${uiLabelMap.FinancialsPaymentReceiptsDetail}</a></li>
      <#assign reportGroupedList = Static["org.opentaps.common.reporting.UtilReports"].getManagedReports(parameters.componentName, "FIN_RECEIVABLES", delegator, Static["org.ofbiz.base.util.UtilHttp"].getLocale(request))?default([])/>
      <#list reportGroupedList as reportGroup>
        <#list reportGroup.reports as report>
          <li class="tabletext"><a href="<@ofbizUrl>setupReport?reportId=${report.reportId}</@ofbizUrl>">${report.shortName}</a></li>
        </#list>
      </#list>
    </ul>
  </p>

</div>
