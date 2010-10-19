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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#assign partyIdTo = parameters.partyIdTo />
<#assign partyIdFrom = parameters.partyIdFrom />

<div class="form">

    <div class="subSectionBlock">
        <@display text="${uiLabelMap.OpentapsMergePartiesConfirmMessage}" class="requiredField" />
    </div>

    <form name="ConfirmMergePartyForm" action="${mergeFormTarget}" method="post">
        <@inputHidden name="partyId" value="${partyIdTo}" />
        <@inputHidden name="partyIdTo" value="${partyIdTo}" />
        <@inputHidden name="partyIdFrom" value="${partyIdFrom}" />
        <table class="twoColumnForm">
            <@displayPartyLinkRow partyId="${partyIdFrom}" title="${fromPartyTitle}" titleClass="requiredField"/>
            <@displayPartyLinkRow partyId="${partyIdTo}" title="${toPartyTitle}" titleClass="requiredField"/>
            <@displayLinkRow href="${mergeFormChangeTarget}?partyIdFrom=${partyIdFrom}&amp;partyIdTo=${partyIdTo}" title="" text="${uiLabelMap.CommonChange}" class="buttontext" />
            <@inputSubmitRow title="${uiLabelMap.OpentapsConfirmMerge}"/>
        </table>
    </form>

</div>
