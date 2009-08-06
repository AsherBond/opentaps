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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<div class="subSectionBlock">

<form action="findPaycheck" name="findPaycheck" method="post">
    <table class="twoColumnForm">

        <@inputHidden name="noConditionFind" value="Y"/>
        <@inputTextRow name="paymentId" title=uiLabelMap.FinancialsPaymentId size=20 maxlength=20 />
        <@inputSelectRow name="paymentTypeId" title=uiLabelMap.FinancialsPaycheckType list=paycheckTypes displayField="description" required=false />
        <@inputAutoCompletePartyRow title=uiLabelMap.FinancialsPayToParty name="partyIdTo" id="findPaycheckFormPartyId" />
        <@inputSelectRow name="statusId" title=uiLabelMap.CommonStatus list=statusList displayField="description" default=parameters.statusId?default("PMNT_SENT") required=false />
        <tr>
            <@displayTitleCell title=uiLabelMap.AccountingEffectiveDate />
            <td>
                ${uiLabelMap.CommonFrom}: <@inputDate name="fromDate" form="findPaycheck" />
                &nbsp;&nbsp;
                ${uiLabelMap.CommonThru}: <@inputDate name="thruDate" form="findPaycheck" />
                <@displayError name="fromDate" />
                <@displayError name="thruDate" />
            </td>
        </tr>
        <@inputSelectRow name="paymentMethodId" title=uiLabelMap.AccountingPaymentMethod list=paymentMethodList key="paymentMethodId" displayField="description" required=false />
        <@inputTextRow name="paymentRefNum" title=uiLabelMap.FinancialsPaymentRefNum />
        <@inputSubmitRow title=uiLabelMap.FinancialsFindPaycheck />

    </table>
</form>

</div>

<div class="subSectionBlock">

    <@paginate name="paychecks" list=paymentListBuilder rememberPage=false>
    <#noparse>

        <@navigationHeader title=uiLabelMap.AccountingPayments />
        <table class="listTable">
            <tr class="listTableHeader">
                <@headerCell title=uiLabelMap.FinancialsPaymentId orderBy="paymentId" />
                <@headerCell title=uiLabelMap.AccountingEffectiveDate orderBy="effectiveDate" />
                <@headerCell title=uiLabelMap.FinancialsPaymentRef orderBy="paymentRefNum" />
                <@headerCell title=uiLabelMap.CommonStatus orderBy="statusId" />
                <@headerCell title=uiLabelMap.FinancialsPayToParty orderBy="partyIdTo" />
                <@headerCell title=uiLabelMap.AccountingAmount orderBy="amount" blockClass="textright" />
                <td class="textright">${uiLabelMap.FinancialsAmountOutstanding}</td>
            </tr>
            <#list pageRows as payment>
              <tr class="${tableRowClass(payment_index)}">
                <@displayLinkCell text=payment.paymentId href="viewPaycheck?paymentId=${payment.paymentId}" />
                <@displayDateCell date=payment.effectiveDate />
                <@displayCell text=payment.paymentRefNum />
                <@displayCell text=payment.statusDescription />
                <@displayCell text=payment.employeeName />
                <@displayCurrencyCell amount=payment.amount />
                <@displayCurrencyCell amount=payment.amountOutstanding />
              </tr>
            </#list>
        </table>

    </#noparse>
    </@paginate>

</div>
