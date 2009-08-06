<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<div class="screenlet" style="position: fixed; bottom:200px; left:8px; width:158px; z-index:1;_position:absolute;_bottom:expression(eval(document.body.scrollBottom)-100);">
  <div class="screenlet-header">
    <span class="boxhead">${uiLabelMap.FinancialsTransactionBalance}</span>
  </div>
  <div class="screenlet-body">
    <#if accountingTransaction?exists>
      <table width="100%" cellpadding="0" cellspacing="2">
        <tr>
          <td><div class="tabletext"><b>${uiLabelMap.FinancialsDebitTotal}</b></div></td>
          <td align="right"><@displayCurrency amount=accountingTransaction.debitTotal /></td>
        </tr>
        <tr>
          <td><div class="tabletext"><b>${uiLabelMap.FinancialsCreditTotal}</b></div></td>
          <td align="right"><@displayCurrency amount=accountingTransaction.creditTotal /></td>
        </tr>
        <tr>
          <td><div class="tabletext"><b>${uiLabelMap.FinancialsStatementsBalance}</b></div></td>
          <td align="right"><@displayCurrency amount=accountingTransaction.trialBalance /></td>
        </tr>
      </table>
    <#else/>
      <div class="tabletext">${uiLabelMap.FinancialsServiceErrorReverseTransactionNotFound} ${acctgTransId!}</div>
    </#if>
  </div>
</div>
