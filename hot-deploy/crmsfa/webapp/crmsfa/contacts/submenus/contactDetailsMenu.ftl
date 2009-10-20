<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<#if hasUpdatePermission?exists>
  <#assign updateLink><a class="subMenuButton" href="updateContactForm?partyId=${partySummary.partyId}">${uiLabelMap.CommonEdit}</a></#assign>
</#if>

  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if isDeactivateLinkRendered?default(false)>  
  <#assign deactivateLink><@inputConfirm class="subMenuButtonDangerous" href="deactivateContact?partyId=${partySummary.partyId}" title=uiLabelMap.CrmDeactivateContact /></#assign>
</#if>

<#if isAssignedToMeLinkRendered?default(false)>
  <#assign assignLink><a class="subMenuButton" href="assignContactToParty?partyId=${parameters.partyId}&roleTypeId=CONTACT">${uiLabelMap.OpentapsAssignToMe}</a></#assign>
</#if>

<#if isUnassignLinkRendered?default(false)>
  <#assign unassignLink><a class="subMenuButton" href="unassignPartyFromContact?partyId=${parameters.partyId}&roleTypeId=CONTACT">${uiLabelMap.OpentapsUnassign}</a></#assign>
</#if>

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmContact}
        <#if contactDeactivated?exists><span class="subSectionWarning">${uiLabelMap.CrmContactDeactivated} ${getLocalizedDate(contactDeactivatedDate, "DATE_TIME")}</span></#if>
    </div>
    <div class="subMenuBar">${assignLink?if_exists}${unassignLink?if_exists}${updateLink?if_exists}${deactivateLink?if_exists}</div>
</div>
