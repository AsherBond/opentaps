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

<style type="text/css">
  tr.disabledApplication, tr.disabledApplication td {
    text-decoration:line-through;
  }
</style>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@form name="processLockboxBatchAction" url="processLockboxBatch" organizationPartyId=organizationPartyId lockboxBatchId=batch.lockboxBatchId />

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.FinancialsLockboxBatchDetail>
    <div style="float: right;">
      <@submitFormLink form="processLockboxBatchAction" text=uiLabelMap.FinancialsProcessLockboxBatch />
    </div>
  </@sectionHeader>
  <#if batch?has_content>
    <div>
      <table class="listTable">
        <tr class="listTableHeader">
          <@displayCell text=uiLabelMap.FinancialsLockboxBatchNumber />
          <@displayCell text=uiLabelMap.CommonDate />
          <@displayCell text=uiLabelMap.FinancialsLockboxOriginalAmount blockClass="titleCell" />
          <@displayCell text=uiLabelMap.FinancialsLockboxPendingAmount blockClass="titleCell"  />
        </tr>
        <tr>
          <@displayLinkCell text=batch.batchId href="viewLockboxBatch?lockboxBatchId=${batch.lockboxBatchId}" />
          <@displayDateCell date=batch.datetimeEntered />
          <@displayCurrencyCell amount=batch.batchAmount />
          <@displayCurrencyCell amount=batch.outstandingAmount />
        </tr>
      </table>
    </div>
  <#else>
    <div class="form">
      <div class="tabletext">${uiLabelMap.FinancialsNoLockboxBatchFound}</div>
    </div>
  </#if>
</div>

<div class="subSectionBlock">
  <@sectionHeader title=uiLabelMap.FinancialsLockboxBatchItems>
    <div style="float: right;">
      <a href="manageLockboxBatches" class="buttontext">${uiLabelMap.CommonBack}</a>
    </div>
  </@sectionHeader>
  <#if batch.lockboxBatchItems?has_content>
    <div>
      <form method="post" name="lockboxBatchItemDetailAction" action="lockboxBatchItemDetailAction">
      <table class="listTable" style="text-align:right">
        <tr class="listTableHeader">
          <@displayCell text=uiLabelMap.CommonType />
          <@displayCell text=uiLabelMap.FinancialsLockboxIdentifier />
          <@displayCell text=uiLabelMap.CommonAmount blockClass="titleCell" />
          <@displayCell text=uiLabelMap.FinancialsLockboxInvoiceAmount blockClass="titleCell" />
          <@displayCell text=uiLabelMap.FinancialsLockboxAmountToApply blockClass="titleCell" />
          <@displayCell text=uiLabelMap.FinancialsLockboxCashDiscount blockClass="titleCell" />
          <td/>
          <@displayCell text=uiLabelMap.CommonStatus />
          <@displayCell text=uiLabelMap.OpentapsCustomer />
        </tr>
        <#assign index=0 />
        <#list batch.lockboxBatchItems as item>
          <#assign index=index+1 />
          <tr class="${tableRowClass(index)}">
            <@displayCell text="CK" />
            <@displayCell text=item.checkNumber />
            <@displayCurrencyCell amount=item.checkAmount />
            <td/>
            <@displayCurrencyCell amount=item.amountToApplyTotal />
            <@displayCurrencyCell amount=item.cashDiscountTotal />
            <td/>
            <td><#if item.isApplied()>Applied<#else><#if item.isReady()>Ready<#else><#if !item.isBalanced()>Not Balanced<#else><#if item.hasError()>Error</#if></#if></#if></#if></td>
            <td/>
          </tr>
          <#list item.lockboxBatchItemDetails as application>
            <#assign index=index+1 />
            <@inputHidden name="lockboxBatchId" value=application.lockboxBatchId index=index />
            <@inputHidden name="itemSeqId" value=application.itemSeqId index=index />
            <@inputHidden name="detailSeqId" value=application.detailSeqId index=index />
            <@inputHiddenRowSubmit submit=false index=index />
            <tr class="${tableRowClass(index)} <#if item.isApplied() && !application.isApplied()>disabledApplication</#if>" <#if item.isApplied()>style="<#if !application_has_next>border-bottom:1px solid #AAAAAA;</#if>"</#if>>
              <td><#if application.isUserEntered()>User inserted</#if> AL</td>
              <td>
                <span class="tabletext">
                  <#if application.hasValidCustomer()><@displayLink text=application.customerId href=application.customer.createViewPageURL(externalKeyParam) /><#else>${application.customerId}</#if> / <#if application.hasValidInvoice()><@displayLink text=application.invoiceNumber href="viewInvoice?invoiceId=${application.invoiceNumber}" /><#else>${application.invoiceNumber?if_exists}</#if></span>
              </td>
              <@displayCurrencyCell amount=application.invoiceAmount />
              <#if application.hasValidInvoice()>
                <td><@displayCurrency amount=application.erpInvoiceOpenAmountInBatch /> / <@displayCurrency amount=application.erpInvoiceAmount /></td>
              <#else>
                <td/>
              </#if>
              <#if application.canUpdate() && !item.isApplied()>
                <@inputTextCell name="amountToApply" index=index size="10" default=application.amountToApply />
                <@inputTextCell name="cashDiscount" index=index size="10" default=application.cashDiscount />
                <td style="text-align:left"><@inputSubmitIndexed title=uiLabelMap.CommonUpdate index=index /></td>
              <#else>
                <#if application.isApplied() && !item.isApplied()>
                  <@displayCurrencyCell amount=application.amountToApply />
                  <@displayCurrencyCell amount=application.cashDiscount />
                <#else>
                  <td/>
                  <td/>
                </#if>
                <td/>
              </#if>
              <#if application.isApplied()>
                <@displayLinkCell text="${uiLabelMap.AccountingPayment} #${application.paymentId}" href="viewPayment?paymentId=${application.paymentId}" />
              <#else>
                <@displayCell text=application.status.message />
              </#if>
              <#if application.customer?exists>
                <@displayLinkCell text=application.customer.name href=application.customer.createViewPageURL(externalKeyParam) />
              <#else>
                <td/>
              </#if>
            </tr>
          </#list>
          <#if !item.isApplied()>
            <#assign index=index+1 />
            <@inputHidden name="lockboxBatchId" value=item.lockboxBatchId index=index />
            <@inputHidden name="itemSeqId" value=item.itemSeqId index=index />
            <@inputHiddenRowSubmit submit=false index=index />
            <tr class="${tableRowClass(index)}" style="border-bottom:1px solid #AAAAAA">
              <@displayCell text="Add AL" />
              <td>
                <@inputAutoCompleteClient name="partyId" id="newDetailPartyId_${index}" size="10" index=index /> / <@inputLookup name="invoiceId" lookup="LookupInvoice" form="lockboxBatchItemDetailAction" size="10" index=index />
              </td>
              <td/>
              <td/>
              <@inputTextCell name="amountToApply" size="10" index=index default="${item.outstandingAmount}" />
              <@inputTextCell name="cashDiscount" size="10" index=index default="0" />
              <td style="text-align:left"><@inputSubmitIndexed title=uiLabelMap.CommonAdd index=index /></td>
              <td/>
              <td/>
            </tr>
          </#if>
        </#list>
      </table>
      <@inputHidden name="_rowCount" value=index+1 />
      <@inputHiddenUseRowSubmit />
      <@inputHidden name="lockboxBatchId" value=batch.lockboxBatchId />
      </form>
    </div>
  <#else>
    <div class="form">
      <div class="tabletext">${uiLabelMap.FinancialsNoLockboxBatchItem}</div>
    </div>
  </#if>
</div>
