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

<div class="subSectionBlock">
    <form name="MergePartyForm" method="post" action="${mergeFormTarget}">
    <table class="twoColumnForm">
        <tr>
            <td class="titleCell"><span class="requiredField">${fromPartyTitle}</span></td>
            <td>
                <@inputAutoComplete name="partyIdFrom" url="${remoteService}" lookup="${lookupTarget}" styleClass="inputAutoCompleteQuick" size="20" />
            </td>
        </tr>
        <tr>
            <td class="titleCell"><span class="requiredField">${toPartyTitle}</span></td>
            <td>
                <@inputAutoComplete name="partyIdTo" url="${remoteService}" lookup="${lookupTarget}" styleClass="inputAutoCompleteQuick" size="20" />
            </td>
        </tr>
        <tr><td colspan="2"></td></tr>
        <tr>
        <td></td>
        <td>
            <@submitFormLinkConfirm form="MergePartyForm" text="${uiLabelMap.OpentapsMerge}"/>
        </td>
        </tr>
    </table>
    </form>
</div>
