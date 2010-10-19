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

<#-- TODO: hasUpdatePermission does not work here because the ready status is not checked properly -->            

<#if (invoice.isCommissionInvoice() || invoice.isPurchaseInvoice() || invoice.isReturnInvoice()) && invoice.isReady() && invoice.getOpenAmount().signum() == 1>
<div class="subSectionBlock">
<form method="post" action="<@ofbizUrl>convertToBillingAccount</@ofbizUrl>" name="convertToBillingAccount">
  <input type="hidden" name="invoiceId" value="${invoice.invoiceId}"/>

  <@sectionHeader title=uiLabelMap.FinancialsApplyToBillingAccount />
  <table class="twoColumnForm">
    <@inputSelectRow name="billingAccountId" title=uiLabelMap.FinancialsCustomerBillingAccount list=billingAccounts required=false defaultOptionText=uiLabelMap.FinancialsCustomerBillingAccountNew ; billingAccount>
    ${billingAccount.description?if_exists} [${billingAccount.billingAccountId}]
    </@inputSelectRow>
    <@inputTextRow name="amount" title=uiLabelMap.CommonAmount default=invoiceOpenAmount />
    <@inputSubmitRow title=uiLabelMap.CommonApply />
  </table>

</form>
</div>
</#if>

