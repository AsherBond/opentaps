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
    <form method="post" action="<@ofbizUrl>findCommissionAgreements</@ofbizUrl>" name="findAgreementForm" style="margin: 0pt;">
        <@inputHidden name="performFind" value="Y"/>
        <#if agreementTypeId?has_content>
            <@inputHidden name="agreementTypeId" value="${agreementTypeId}"/>
        </#if>
        <#if partyIdFrom?has_content>
            <@inputHidden name="partyIdFrom" value="${partyIdFrom}"/>
        </#if>
        <#if roleTypeIdFrom?has_content>
            <@inputHidden name="roleTypeIdFrom" value="${roleTypeIdFrom}"/>
        </#if>
        <#if roleTypeIdTo?has_content>
            <@inputHidden name="roleTypeIdTo" value="${roleTypeIdTo}"/>
        </#if>
        <#if parameters.partyId?has_content>
            <#assign partyIdTo = parameters.partyId/>
        </#if>
        <table class="twoColumnForm">
            <@inputAutoCompletePartyRow title="${uiLabelMap.AccountingToParty}" name="partyIdTo" id="findAgreementFormPartyId" />
            <@inputSelectRow name="statusId" list=statuses?default([]) title="${uiLabelMap.CommonStatus}" required=false ; option>
            	${option.get("description", locale)}
            </@inputSelectRow>
            <@inputSubmitRow title="${uiLabelMap.CommonFind}"/>
        </table>
    </form>
</div>
