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
      <@inputSelectRow title=uiLabelMap.AccountingPaymentType name="paymentTypeId" list=paymentTypeList key="paymentTypeId" displayField="description" required=false/>
      
      <#if findDisbursement>
        <@inputAutoCompletePartyRow title=uiLabelMap.FinancialsPayToParty name="partyIdTo" id="findPaymentFormPartyId" />
      <#else>
        <@inputAutoCompletePartyRow title=uiLabelMap.FinancialsReceiveFromParty name="partyIdFrom" id="findPaymentFormPartyId" />
      </#if>

      <@inputSelectRow title=uiLabelMap.FinancialsStatusId name="statusId" list=statusList key="statusId" displayField="description" required=false/>
      
      <#if findDisbursement>
        <@inputSelectRow title=uiLabelMap.FinancialsPaymentMethod name="paymentMethodId" list=paymentMethodList key="paymentMethodId" displayField="description" required=false/>
      <#else>
        <@inputSelectRow title=uiLabelMap.FinancialsPaymentMethodType name="paymentMethodTypeId" list=paymentMethodTypeList key="paymentMethodTypeId" displayField="description" required=false/>
      </#if>

      <@inputDateRangeRow title=uiLabelMap.AccountingEffectiveDate fromName="fromDate" thruName="thruDate" />

      <@inputTextRow title=uiLabelMap.FinancialsPaymentRefNum name="paymentRefNum" />

      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>
</@frameSection>
