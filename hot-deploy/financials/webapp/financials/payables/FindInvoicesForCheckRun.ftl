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

<div class="subSectionBlock">
    <@sectionHeader title="${uiLabelMap.FinancialsCheckRun}"/>

    <form method="post" action="checkRun" onSubmit="javascript:submitFormDisableSubmits(this)" name="FindInvoicesForCheckRun">
        <table class="twoColumnForm">
            <@inputSelectRow title=uiLabelMap.AccountingPaymentMethod name="paymentMethodId" list=checkPaymentMethods key="paymentMethodId" displayField="description" default="${defaultPaymentMethodId}" titleClass="requiredField"/>
            <@inputTextRow title=uiLabelMap.FinancialsInitialCheckNumber name="initialCheckNumber" size="20" titleClass="requiredField"/>
            <#if tagTypes?has_content>
                <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=paymentValue! />
            </#if>
            <tr><td colspan="2">&nbsp;</td></tr>
            <@inputLookupRow title=uiLabelMap.AccountingFromParty name="partyIdFrom" lookup="LookupPartyName" form="FindInvoicesForCheckRun"/>
            <@inputDateRow title=uiLabelMap.FinancialsDueDateBefore name="dueDate"/>
            <@inputSubmitRow title=uiLabelMap.CommonFind/>
        </table>
    </form>

</div>