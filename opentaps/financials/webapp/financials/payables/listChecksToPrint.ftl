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

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.FinancialsPrintChecksFor} 
      <#if checkPaymentMethods?has_content>
        <@selectAction name="listChecksToPrint" prompt="${uiLabelMap.FinancialSelectPaymentMethod}">
          <#list checkPaymentMethods as paymentMethod>
            <#assign select = false/>
            <#if paymentMethod.paymentMethodId = paymentMethodId><#assign select = true/></#if>
            <@action url="listChecksToPrint?paymentMethodId=${paymentMethod.paymentMethodId}" text="${paymentMethod.description?if_exists} (${paymentMethod.paymentMethodId})" selected=select/>
          </#list>
        </@selectAction>
      </#if>
    </div>
</div>

<@paginate name="listChecksToPrint" list=paymentListBuilder formAction=formAction>
<#noparse>
<@navigationBar/>
<form method="post" action="<@ofbizUrl>check.pdf</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="ListPayments">
<table class="listTable">
    <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.AccountingEffectiveDate orderBy="effectiveDate" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.AccountingPaymentID orderBy="paymentId" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentMethod orderBy="paymentMethodId" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.FinancialsPaymentRefNum orderBy="paymentRefNum" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.FinancialsPayToParty orderBy="partyIdTo" blockClass="tableheadtext"/>
        <@headerCell title=uiLabelMap.AccountingAmount orderBy="amount" blockClass="tableheadtext"/>
        <td><span class="tableheadtext">${uiLabelMap.CommonSelect}<br><input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'ListPayments');"/></span></td>
    </tr>
    <#list pageRows as payment>
        <#if (payment_index % 2) == 0><#assign rowStyle = "viewManyTR1"/><#else><#assign rowStyle = "viewManyTR2"/></#if>
        <tr class="${rowStyle}">
            <@displayDateCell date=payment.effectiveDate format="DATE" />    
            <@displayLinkCell href="${payment.view}?paymentId=${payment.paymentId}" class="linktext" text="${payment.paymentId}"/>
            <@displayCell text="${payment.paymentMethod} (${payment.paymentMethodId})" />            
            <@displayCell text=payment.paymentRefNum?default("") />    
            <@displayCell text="${payment.partyToName}" />
            <td><@displayCurrency amount=payment.amount currencyUomId=payment.currencyUomId /></td>
            <@inputHidden name="paymentId" value="${payment.paymentId}" index=payment_index/>                        
            <@inputHidden name="paymentMethodId" value="${payment.paymentMethodId}" index=payment_index/>
            <@inputHidden name="paymentRefNum" value="${payment.paymentRefNum?default(\"\")}" index=payment_index/>
            <@inputHidden name="effectiveDate" value="${getLocalizedDate(payment.effectiveDate)}" index=payment_index/>
            <@inputHidden name="partyIdTo" value="${payment.partyIdTo}" index=payment_index/>
            <@inputHidden name="amount" value="${payment.amount}" index=payment_index/>
            <td><div class="tabletext"><input type="checkbox" name="_rowSubmit_o_${payment_index}" value="Y"/> </div></td>
        </tr>
    </#list>
    <#if pageSize != 0>
      <tr>
        <td colspan="7" align="right">
        <a href="javascript:document.ListPayments.submit();" class="subMenuButton">${uiLabelMap.FinancialsPrintChecks}</a>
        <a href="javascript:document.ListPayments.action='quickSendPayment'; document.ListPayments.submit();" class="subMenuButton">${uiLabelMap.FinancialsPaymentStatusToSent}</a>
        </td>
      </tr>
    </#if>
</table>
<#if pageRows?has_content>
    <#assign paymentMethodId = pageRows.get(0).paymentMethodId/>
<#else>
    <#assign paymentMethodId = ""/>
</#if>
<@inputHidden name="paymentMethodId" value=paymentMethodId/>
<@inputHidden name="_useRowSubmit" value="Y"/>
<@inputHidden name="_rowCount" value="${pageRows?size}"/>
</form>
</#noparse>
</@paginate>
