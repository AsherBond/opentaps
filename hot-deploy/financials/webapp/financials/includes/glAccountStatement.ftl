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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#include "generateStatement.ftl" />

<#if transactions?exists>

<div style="border: 1px solid #999999; margin-top: 20px; margin-bottom: 20px;"></div>
<table class="tabletext">
  <tr>
    <td class="tableheadtext" align="center" colspan="4">${uiLabelMap.FinancialsTaxesFor} ${glAccount.accountCode}: ${glAccount.accountName} (${glAccount.glAccountId})</td>
  </tr>
  <tr>
    <td align="center" colspan="4"><b>${uiLabelMap.CommonFrom}</b> ${getLocalizedDate(fromDate)} <b>${uiLabelMap.CommonThru}</b> ${getLocalizedDate(thruDate)}</td>
  </tr>
  <tr><td colspan="4">&nbsp;</td></tr>

  <tr>
    <td>${uiLabelMap.FinancialsTotalTaxLiabilities}</td>
    <td colspan="3" align="right"><@ofbizCurrency amount=liabilityAmount isoCode=orgCurrencyUomId /> </td>
  </tr>
  <tr>
    <td>${uiLabelMap.FinancialsTotalTaxPayments}</td>
    <td colspan="3" align="right"><@ofbizCurrency amount=paymentAmount isoCode=orgCurrencyUomId /> </td>
  </tr>
  <tr><td colspan="4">&nbsp;</td></tr>

  <tr><td class="tableheadtext" align="center" colspan="4">${uiLabelMap.FinancialsTaxTransactions}</td></tr>
  <@generateStatementOld transactions=transactions showBalances=false />

  <tr><td colspan="4">&nbsp;</td></tr>
  <tr><td class="tableheadtext" align="center" colspan="4">${uiLabelMap.FinancialsTaxPayments}</td></tr>
  <@generateStatementOld transactions=paymentTransactions showBalances=false />

</table>

</#if>
