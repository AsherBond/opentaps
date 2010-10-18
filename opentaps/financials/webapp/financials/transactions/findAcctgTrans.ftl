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

<form name="findAcctgTransForm" method="post" action="">
  <@inputHidden name="performFind" value="Y"/>
  <table class="twoColumnForm">
    <tbody>
      <@inputTextRow title=uiLabelMap.FinancialsTransactionId name="findAcctgTransId" size="20" maxlength="20"/>
      <@inputSelectRow title=uiLabelMap.FinancialsTransactionType required=false list=transactionTypes  displayField="description" name="acctgTransTypeId" default=acctgTransTypeId?if_exists />
      <@inputAutoCompletePartyRow title="${uiLabelMap.PartyPartyId}" name="partyId" />
      <@inputSelectRow title=uiLabelMap.FinancialsGlFiscalType required=false list=glFiscalTypes  displayField="description" name="glFiscalTypeId" default=glFiscalTypeId?if_exists />
      <@inputRangeRow title=uiLabelMap.FinancialsPostedAmount fromName="postedAmountFrom" thruName="postedAmountThru" size=10/>
      <@inputIndicatorRow name="isPosted" title=uiLabelMap.FinancialsIsPosted required=true default=isPosted?if_exists />
      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </tbody>
  </table>
</form>
