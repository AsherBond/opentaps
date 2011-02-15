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

<#if invoices?has_content>
  <table class="listTable" >
    <tr class="boxtop">
      <td><span class="boxhead">${uiLabelMap.AccountingInvoice}</span></td>
      <td><span class="boxhead">${uiLabelMap.FinancialsPaymentRef}</span></td>
      <td><span class="boxhead">${uiLabelMap.CommonDescription}</span></td>
      <td><span class="boxhead">${uiLabelMap.AccountingInvoiceDate}</span></td>
      <td><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
      <td><span class="boxhead">${uiLabelMap.AccountingAmountApplied}</span></td>
      <td><span class="boxhead">${uiLabelMap.CommonApply}</span></td>
    </tr>
    <#if invoices?has_content>
      <#list invoices as row>
        <form name="invoices_${row_index}" action="payInvoiceWithBillingAccount" method="POST" class="basic-form">
          <tr class="viewManyTR2">
            <@inputHidden name="billingAccountId" value=billingAccountId />
            <@inputHidden name="invoiceId" value=row.invoiceId?if_exists />
            <@inputHidden name="limitToAvailableBalance" value="Y" />
            <@displayLinkCell text=row.invoiceId href="viewInvoice?invoiceId=${row.invoiceId}"/>
            <@displayCell text=row.invoiceRefNum/>
            <@displayCell text=row.description/>
            <@displayDateCell date=row.invoiceDate/>
            <@displayCell text=row.outstandingAmount/>
            <@inputTextCell name="captureAmount" default=row.amountToApply/>
            <@inputButtonCell title=uiLabelMap.CommonApply/>
          </tr>
          <#if tagTypes?has_content && allocatePaymentTagsToApplications>
            <#-- use the first invoice item tags as default for the application -->
            <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="2" entity=row.firstItem! />
          </#if>		    
        </form>
      </#list>
    </#if>	
  </table>
<#else>
  <#if !hasBalance>
    <div class="tabletext">${uiLabelMap.FinancialsBillingAccountFullyUsed}.</div>
  <#else>
    <div class="tabletext">${uiLabelMap.AccountingNoInvoicesFound}.</div>
  </#if>
</#if>
