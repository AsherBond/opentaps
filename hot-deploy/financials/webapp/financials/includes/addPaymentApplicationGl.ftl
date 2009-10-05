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
 *  @author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="POST" name="addPaymentApplicationGl" action="createPaymentApplication"> <#-- action set by the screen -->
  <input type="hidden" name="paymentId" value="${paymentId}"/>
  <div class="form" style="border:0">
    <table class="twoColumnForm" style="border:0">
      <tr>
        <@displayTitleCell title=uiLabelMap.AccountingGlAccount titleClass="requiredField" />
        <td><@inputAutoCompleteGlAccount name="overrideGlAccountId" id="overrideGlAccountId" /></td>
      </tr>
      <@inputTextRow title=uiLabelMap.CommonAmount size="10" name="amountApplied" titleClass="requiredField" />
      <@inputSubmitRow title=uiLabelMap.CommonApply />
    </table>
  </div>
</form>
