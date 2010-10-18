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
 *  
 *  @author Leon Torres (leon@opensourcestrategies.com)
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form method="POST" name="createQuickAcctgTransForm" action="${quickAcctgTransFormTarget}"> <#-- action set by the screen -->
  <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
  <input type="hidden" name="glFiscalTypeId" value="ACTUAL"/>
  <div class="form" style="border:0">
    <table class="fourColumnForm" style="border:0">
      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsTransactionType />
        <@inputSelectCell list=transactionTypes displayField="description" name="acctgTransTypeId" default=acctgTransTypeId?if_exists />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsTransactionDate />
        <@inputDateTimeCell name="transactionDate" default=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.PartyPartyId />
        <@inputLookupCell lookup="LookupPartyName" form="reconcileForm" name="partyId" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.CommonDescription />
        <@inputTextCell name="description" />
      </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.FinancialsDebitAccount titleClass="requiredField" />
        <@inputAutoCompleteGlAccountCell name="debitGlAccountId" default=debitGlAccountId?if_exists />
        <@displayTitleCell title=uiLabelMap.FinancialsCreditAccount titleClass="requiredField" />
        <@inputAutoCompleteGlAccountCell name="creditGlAccountId" default=creditGlAccountId?if_exists />
      </tr>

      <#list tagTypes as tag>
        <#if tag.isRequired()>
          <#assign titleClass="requiredField" />
        <#else/>
          <#assign titleClass="tableheadtext" />
        </#if>
        
        <tr>
          <@displayTitleCell title=tag.description titleClass=titleClass />
          <@inputSelectCell name="debitTagEnumId${tag.index}" errorField="acctgTagEnumId${tag.index}" list=tag.activeTagValues key="enumId" required=false default=tag.defaultValue! ; tagValue>
            ${tagValue.description}
          </@inputSelectCell>

          <@displayTitleCell title=tag.description titleClass=titleClass />
          <@inputSelectCell name="creditTagEnumId${tag.index}" errorField="acctgTagEnumId${tag.index}" list=tag.activeTagValues key="enumId" required=false default=tag.defaultValue!  ; tagValue>
            ${tagValue.description}
          </@inputSelectCell>
        </tr>
      </#list>

      <tr>
        <@displayTitleCell title=uiLabelMap.CommonAmount titleClass="requiredField" />
        <@inputTextCell size="10" name="amount" />
      </tr>
      <@inputSubmitRow title=uiLabelMap.CommonCreate />
    </table>
  </div>
</form>
