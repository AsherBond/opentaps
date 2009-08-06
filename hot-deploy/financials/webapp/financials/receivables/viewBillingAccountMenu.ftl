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
 *  
-->

<div class="screenlet-header">
  <div style="float: right;"><a class="buttontext" href="<@ofbizUrl>prepareBillingAccountCreditMemo?billingAccountId=${billingAccount.billingAccountId}&amp;organizationPartyId=${parameters.organizationPartyId}</@ofbizUrl>">${uiLabelMap.AccountingInvoicePDF}</a><a class="buttontext" href="<@ofbizUrl>payInvoiceWithBillingAccountForm?billingAccountId=${billingAccount.billingAccountId}</@ofbizUrl>">${uiLabelMap.FinancialsPayInvoice}</a></div>
  <div class="boxhead">${uiLabelMap.FinancialsCustomerBillingAccount}</div>
</div>
