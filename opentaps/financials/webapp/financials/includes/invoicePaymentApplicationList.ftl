<#--
 * Copyright (c) Open Source Strategies, Inc.
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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<table class="listTable" >
  <tr class="boxtop">
    <td><span class="boxhead">${uiLabelMap.AccountingInvoice}</span></td>
    <td><span class="boxhead">${uiLabelMap.FinancialsPaymentRef}</span></td>
    <td><span class="boxhead">${uiLabelMap.CommonDescription}</span></td>
    <td><span class="boxhead">${uiLabelMap.AccountingInvoiceDate}</span></td>
    <td><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
    <td><span class="boxhead">${uiLabelMap.AccountingAmountApplied}</span></td>
    <#assign invoiceProcessing = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("AccountingConfig.properties", "invoiceProcessing")>
      <#if invoiceProcessing.equals("Y")>
	<td><span class="boxhead">${uiLabelMap.invoiceProcessing}</span></td>
      </#if>		    
      <#if invoiceProcessing.equals("N")>
	<td><span class="boxhead">${uiLabelMap.CommonApply}</span></td>
      </#if>	
      <td><span class="boxhead">${uiLabelMap.CommonNote}</span></td>
      <td><span class="boxhead"></span></td>
  </tr>
  <#if invoices?has_content>
    <#list invoices as row>
      <form name="invoices_${row_index}" action="updatePaymentApplication" method="POST" class="basic-form">
        <tr class="viewManyTR2">
          <@inputHidden name="paymentId" value=paymentId?if_exists />
          <@inputHidden name="checkForOverApplication" value="true" />
          <@inputHidden name="invoiceId" value=row.invoiceId?if_exists />
          <@inputHidden name="invoiceRefNum" value=row.invoiceRefNum?if_exists />
          <@inputHidden name="description" value=row.description?if_exists />
          <@inputHidden name="invoiceDate" value=row.invoiceDate?if_exists />
          <@inputHidden name="amount" value=row.amount?if_exists />
          <@inputHidden name="dummy" value=row.outstandingAmount?if_exists />
          <@displayLinkCell text=row.invoiceId href="viewInvoice?invoiceId=${row.invoiceId}"/>
          <@displayCell text=row.invoiceRefNum/>
          <@displayCell text=row.description/>
          <@displayDateCell date=row.invoiceDate/>
          <@displayCell text=row.outstandingAmount/>
          <@inputTextCell name="amountApplied" default=row.amountToApply/>
          <@inputTextCell name="note" ignoreParameters=true default=row.note/>
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
