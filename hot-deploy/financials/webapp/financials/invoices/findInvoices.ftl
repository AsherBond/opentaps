<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="post" action="<@ofbizUrl>${formTarget}</@ofbizUrl>" name="findInvoiceForm" style="margin: 0pt;">
  <@inputHidden name="invoiceTypeId" value="${invoiceTypeId}"/>
  <@inputHidden name="performFind" value="Y"/>
  <table class="twoColumnForm">
    <tbody>
      <@inputTextRow title=uiLabelMap.FinancialsInvoiceId name="invoiceId" size="20" maxlength="20"/>

      <#if isReceivable>
        <@inputHidden name="partyIdFrom" value=parameters.organizationPartyId! />
        <@inputAutoCompletePartyRow title=uiLabelMap.AccountingToParty name="partyId" id="findInvoiceFormPartyId" />
      </#if>

      <#if isPayable>
        <@inputHidden name="partyId" value=parameters.organizationPartyId! />
        <@inputAutoCompletePartyRow title=uiLabelMap.AccountingFromParty name="partyIdFrom" id="findInvoiceFormPartyId" />
      </#if>

      <#if isPartner>
        <@inputSelectRow title=uiLabelMap.OpentapsPartner name="partyIdFrom" list=partners key="partyId" required=false ; partner >
          ${partner.firstName?if_exists} ${partner.lastName?if_exists} ${partner.groupName?if_exists} (${partner.partyId})
        </@inputSelectRow>
      </#if>

      <@inputSelectRow title=uiLabelMap.CommonStatus name="statusId" list=statuses key="statusId" required=false ; status >
        ${status.statusDescription}
      </@inputSelectRow>

      <@inputSelectRow title=uiLabelMap.FinancialsProcessingStatus name="processingStatusId" list=processingStatuses key="statusId" required=false ; status >
        ${status.statusDescription}
      </@inputSelectRow>

      <@inputDateRangeRow title=uiLabelMap.AccountingInvoiceDate fromName="invoiceDateFrom" thruName="invoiceDateThru" />
      <@inputDateRangeRow title=uiLabelMap.AccountingDueDate fromName="dueDateFrom" thruName="dueDateThru" />
      <@inputDateRangeRow title=uiLabelMap.AccountingPaidDate fromName="paidDateFrom" thruName="paidDateThru" />

      <@inputRangeRow title=uiLabelMap.CommonAmount fromName="amountFrom" thruName="amountThru" size=10/>
      <@inputRangeRow title=uiLabelMap.OpentapsOpenAmount fromName="openAmountFrom" thruName="openAmountThru" size=10/>

      <@inputTextRow title=uiLabelMap.FinancialsReferenceNumber name="referenceNumber" size="30"/>

      <#if enableFindByOrder>
        <@inputTextRow title=uiLabelMap.FinancialsRelatedOrderId name="orderId" size="20" maxlength="20"/>
      </#if>

      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </tbody>
  </table>
</form>
