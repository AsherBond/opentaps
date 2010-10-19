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

<form action="findReturns" name="findReturns" method="post">
    <table class="twoColumnForm">
        <@inputHidden name="performFind" value="Y"/>
        <@inputTextRow name="returnId" title=uiLabelMap.OrderReturnId size=20 maxlength=20 />
        <@inputLookupRow name="partyId" title=uiLabelMap.CrmReturnFromCustomer form="findReturns" lookup="LookupPartyName" />
        <@inputSelectRow name="statusId" title=uiLabelMap.CommonStatus list=statusList displayField="description" default=parameters.statusId required=false />
        <@inputSubmitRow title=uiLabelMap.CrmFindReturns />
    </table>
</form>

</div>
