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

<style type="text/css">
  div.allSubSectionBlocks {width: 990px;}
</style>

<#-- display each GL account and the appropriate button next to it -->
<#macro displayAccount account>
<tr>
 <td class="tabletext" width="350" nowrap="nowrap">
  <#if parameters.hasFinancialsTransactionPermission><a href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${account.glAccountId}&organizationPartyId=${parameters.organizationPartyId}</@ofbizUrl>"></#if>
  <#if account.accountName?has_content>
    ${account.accountName}
  <#else/>
    ${account.glAccountTypeId!} - ${account.glAccountClassId!} (${account.glAccountId})
  </#if>
  <#if parameters.hasFinancialsTransactionPermission></a></#if>
</td>
  <td align="right" class="tabletext"><@ofbizCurrency amount=account.postedBalance?default(0.0) isoCode=orgCurrencyUomId /></td>
  <td width="30">
   <#if account.glAccountTypeId="UNDEPOSITED_RECEIPTS">
          <#if parameters.hasFinancialsTransactionPermission>
          (<a href="settleUndepositedPayments?amount=${account.postedBalance?default("")}" class="linktext">${uiLabelMap.FinancialsSettle}</a>)
          </#if>
   <#elseif account.glAccountTypeId="MRCH_STLMNT_ACCOUNT">
          <#assign cardType = delegator.findByAnd("CreditCardTypeGlAccount", Static["org.ofbiz.base.util.UtilMisc"].toMap("glAccountId", account.glAccountId, "organizationPartyId", parameters.organizationPartyId))?first>
          <#if parameters.hasFinancialsTransactionPermission>
          (<a href="settleCreditCardPayments?paymentOrRefund=PAYMENT&cardType=${cardType.cardType}&amount=${account.postedBalance?default("")}" class="linktext">${uiLabelMap.FinancialsSettle}</a>)
          </#if>
   </#if>
  </td>
</tr>
</#macro>

<@frameSectionTitleBar title="${parameters.organizationName} ${uiLabelMap.FinancialsAtAGlance}" titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_${sectionName?if_exists}"/>

<#if chartImage?exists>
<div style="float: right; margin-top: 20px; margin-bottom: 20px;" align="right">
  <img src="<@ofbizUrl>showChart?chart=${chartImage?if_exists?html}</@ofbizUrl>" style="margin-right: 15px; "/>
</div>
</#if>

  <div class="form">
    <#if parameters.hasFinancialsReceivablesPermission>
    <div class="tableheadtext" style="margin-top: 20px;">${uiLabelMap.FinancialsCashEquivalents}</div>
    <table>
       <#list cashEquivalentAccounts?default([]) as account>
          <@displayAccount account/>
       </#list>
    </table>
    <div class="tableheadtext" style="margin-top: 20px;">${uiLabelMap.FinancialsReceivables}</div>
       <table>
        <#list creditCardAccounts?default([]) as account>
          <@displayAccount account/>
       </#list>
       <#list accountsReceivablesAccounts?default([]) as account>
          <@displayAccount account/>
       </#list>
    </table>
    </#if>

    <#if parameters.hasFinancialsReportsPermission>
    <div class="tableheadtext" style="margin-top: 20px;">${uiLabelMap.FinancialsBalanceSheet}</div>
    <table>
      <#list inventoryAccounts?default([]) as account>
          <@displayAccount account/>
      </#list>
    </table>
    </#if>

    <#if parameters.hasFinancialsPayablesPermission>
    <div class="tableheadtext" style="margin-top: 20px;">${uiLabelMap.FinancialsPayables}</div>
    <table>
      <#list accountsPayablesAccounts?default([]) as account>
          <@displayAccount account/>
      </#list>
    </table>
    </#if>

    <#if chartImage?exists>
      <!-- so that the floating chart does not spill out the div -->
      <div style="clear:both"></div>
    </#if>

  </div>
<br/>
