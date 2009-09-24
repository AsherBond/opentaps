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

<#if hasTeamDeactivatePermission?exists>
<@form name="deactivateTeamForm" url="deactivateTeam" partyId="${team.partyId}" />
<#assign deactivateLink><@submitFormLinkConfirm form="deactivateTeamForm" class='subMenuButtonDangerous' text=uiLabelMap.CrmDeactivateTeam/></#assign>
</#if>
<#if hasTeamUpdatePermission?exists>
<#assign updateLink = "<a class='subMenuButton' href='updateTeamForm?partyId=" + team.partyId + "'>" + uiLabelMap.CommonEdit + "</a>">
</#if>

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmTeam}
        <#if teamDeactivated?exists><span class="subSectionWarning">${uiLabelMap.CrmTeamDeactivated}</span></#if>
    </div>
    <div class="subMenuBar">${updateLink?if_exists}${deactivateLink?if_exists}</div>
</div>
