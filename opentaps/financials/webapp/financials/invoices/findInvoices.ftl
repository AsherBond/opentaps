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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="post" action="<@ofbizUrl>${formTarget}</@ofbizUrl>" name="findInvoiceForm" style="margin: 0pt;">
  <@inputHidden name="invoiceTypeId" value="${invoiceTypeId}"/>
  <@inputHidden name="performFind" value="Y"/>
  <table class="fourColumnForm">
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

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonStatus />
        <@inputSelectCell name="statusId" list=statuses key="statusId" required=false ; status >
          ${status.statusDescription}
        </@inputSelectCell>

        <@displayTitleCell title=uiLabelMap.FinancialsProcessingStatus />
        <@inputSelectCell name="processingStatusId" list=processingStatuses key="statusId" required=false ; status >
          ${status.statusDescription}
        </@inputSelectCell>
      </tr>

      <@inputDateRangeRow title=uiLabelMap.AccountingInvoiceDate fromName="invoiceDateFrom" thruName="invoiceDateThru" />

      <tr>
        <@displayTitleCell title=uiLabelMap.AccountingDueDate />
        <@inputDateRangeCell fromName="dueDateFrom" thruName="dueDateThru" />

        <@displayTitleCell title=uiLabelMap.AccountingPaidDate />
        <@inputDateRangeCell fromName="paidDateFrom" thruName="paidDateThru" />
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonAmount />
        <@inputRangeCell fromName="amountFrom" thruName="amountThru" size=10/>

        <@displayTitleCell title=uiLabelMap.OpentapsOpenAmount />
        <@inputRangeCell fromName="openAmountFrom" thruName="openAmountThru" size=10/>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsReferenceNumber />
        <@inputTextCell name="referenceNumber" size="30"/>

        <#if enableFindByOrder>
          <@displayTitleCell title=uiLabelMap.FinancialsRelatedOrderId />
          <@inputTextCell name="orderId" size="20" maxlength="20"/>
        </#if>
      </tr>

      <@inputTextRow title=uiLabelMap.CommonMessage name="message" size="30"/>
      <@inputTextRow title=uiLabelMap.ProductItemDescription name="itemDescription" size="30"/>

      <#if tagFilters?has_content>
        <#list tagFilters as tag>
          <@inputSelectRow title=tag.description name="tag${tag.index}" list=tag.activeTagValues key="enumId" required=true ; tagValue>
            ${tagValue.description}
          </@inputSelectRow>
        </#list>
      </#if>

      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </tbody>
  </table>
</form>
