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

<@frameSection title=uiLabelMap.FinancialsFindPayment>
  <form method="post" action="<@ofbizUrl>findPayment</@ofbizUrl>" name="findPayment">
    <@inputHidden name="findPaymentTypeId" value="${parameters.findPaymentTypeId?if_exists}"/>
    <#if findDisbursement>
      <@inputHidden name="partyIdFrom" value="${organizationPartyId?if_exists}"/>
    <#else>
      <@inputHidden name="partyIdTo" value="${organizationPartyId?if_exists}"/>
    </#if>
    <table class="twoColumnForm">
      <@inputTextRow title=uiLabelMap.FinancialsPaymentId name="paymentId" />
      
      <#if findDisbursement>
        <@inputAutoCompletePartyRow title=uiLabelMap.FinancialsPayToParty name="partyIdTo" id="findPaymentFormPartyId" />
      <#else>
        <@inputAutoCompletePartyRow title=uiLabelMap.FinancialsReceiveFromParty name="partyIdFrom" id="findPaymentFormPartyId" />
      </#if>

      <@inputSelectRow title=uiLabelMap.FinancialsStatusId name="statusId" list=statusList key="statusId" displayField="description" required=false/>
      
      <tr>
        <@displayTitleCell title=uiLabelMap.AccountingPaymentType />
        <@inputSelectCell name="paymentTypeId" list=paymentTypeList key="paymentTypeId" displayField="description" required=false/>

        <#if findDisbursement>
          <@displayTitleCell title=uiLabelMap.FinancialsPaymentMethod />
          <@inputSelectCell name="paymentMethodId" list=paymentMethodList key="paymentMethodId" displayField="description" required=false/>
        <#else>
          <@displayTitleCell title=uiLabelMap.FinancialsPaymentMethodType />
          <@inputSelectCell name="paymentMethodTypeId" list=paymentMethodTypeList key="paymentMethodTypeId" displayField="description" required=false/>
        </#if>
      </tr>

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonAmount />
        <@inputRangeCell fromName="amountFrom" thruName="amountThru" size=10/>

        <@displayTitleCell title=uiLabelMap.OpentapsOpenAmount />
        <@inputRangeCell fromName="openAmountFrom" thruName="openAmountThru" size=10/>
      </tr>

      <@inputDateRangeRow title=uiLabelMap.AccountingEffectiveDate fromName="fromDate" thruName="thruDate" />

      <@inputTextRow title=uiLabelMap.FinancialsPaymentRefNum name="paymentRefNum" />

      <#if tagFilters?has_content>
        <#list tagFilters as tag>
          <@inputSelectRow title=tag.description name="tag${tag.index}" list=tag.activeTagValues key="enumId" required=true ; tagValue>
            ${tagValue.description}
          </@inputSelectRow>
        </#list>
      </#if>

      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>
</@frameSection>
