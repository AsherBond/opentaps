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

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.FinancialsPaymentsToConfirm}</div>
</div>

<@paginate name="listChecksToConfirm" list=paymentListBuilder formAction=formAction>
<#noparse>
<@navigationBar/>
<form method="post" action="<@ofbizUrl>confirmSentPayments</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="ListPayments">
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
            <#if payment.paymentMethod?exists>
                <@displayCell text="${payment.paymentMethod} (${payment.paymentMethodId})" />
            <#else>
                <@displayCell text=uiLabelMap.OpentapsUnknown />
            </#if>
            <@displayCell text=payment.paymentRefNum?default("") />
            <@displayCell text="${payment.partyToName}" />
            <td><@displayCurrency amount=payment.amount currencyUomId=payment.currencyUomId /></td>
            <@inputHidden name="paymentId" value="${payment.paymentId}" index=payment_index/>                        
            <@inputHidden name="statusId" value="PMNT_CONFIRMED" index=payment_index/>
            <td><div class="tabletext"><input type="checkbox" name="_rowSubmit_o_${payment_index}" value="Y"/> </div></td>
        </tr>
    </#list>
    <#if pageSize != 0>
      <tr><td colspan="5">&nbsp;</td><td colspan="2"><@inputSubmit title=uiLabelMap.FinancialsConfirmPayments /></td></tr>
    </#if>
</table>
<@inputHidden name="_useRowSubmit" value="Y"/>
<@inputHidden name="_rowCount" value="${pageRows?size}"/>
</form>
</#noparse>
</@paginate>
