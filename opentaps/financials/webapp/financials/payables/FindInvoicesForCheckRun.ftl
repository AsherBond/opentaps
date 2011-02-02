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

<div class="subSectionBlock">
    <@sectionHeader title="${uiLabelMap.FinancialsCheckRun}"/>

    <form method="post" action="checkRun" onSubmit="javascript:submitFormDisableSubmits(this)" name="FindInvoicesForCheckRun">
        <table class="twoColumnForm">
            <@inputSelectRow title=uiLabelMap.FinancialsPaymentMethod name="paymentMethodId" list=checkPaymentMethods key="paymentMethodId" displayField="description" default="${defaultPaymentMethodId?if_exists}" titleClass="requiredField"/>
            <@inputTextRow title=uiLabelMap.FinancialsInitialCheckNumber name="initialCheckNumber" size="20" titleClass="requiredField"/>
            <#if tagTypes?has_content>
                <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=paymentValue! />
            </#if>
            <tr><td colspan="2">&nbsp;</td></tr>
            <@inputLookupRow title=uiLabelMap.OpentapsFromParty name="partyIdFrom" lookup="LookupPartyName" form="FindInvoicesForCheckRun"/>
            <@inputDateRow title=uiLabelMap.FinancialsDueDateBefore name="dueDate"/>
            <@inputSubmitRow title=uiLabelMap.CommonFind/>
        </table>
    </form>

</div>
