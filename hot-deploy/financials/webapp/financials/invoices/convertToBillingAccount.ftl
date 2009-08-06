<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

