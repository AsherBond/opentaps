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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if isDeactivateLinkRendered?default(false)>  
  <@form name="DeactivateContactHiddenForm" url="deactivateContact" partyId="${partySummary.partyId}" />
</#if>

<#if isAssignedToMeLinkRendered?default(false)>
  <@form name="AssignContactHiddenForm" url="assignContactToParty" partyId="${parameters.partyId}" roleTypeId="CONTACT" />
</#if>

<#if isUnassignLinkRendered?default(false)>
  <@form name="UnassignContactHiddenForm" url="unassignPartyFromContact" partyId="${parameters.partyId}" roleTypeId="CONTACT" />
</#if>

<#assign frameTitle>
  ${uiLabelMap.CrmContact}
  <#if contactDeactivated?exists><span class="subSectionWarning">${uiLabelMap.CrmContactDeactivated} ${getLocalizedDate(contactDeactivatedDate, "DATE_TIME")}</span></#if>
</#assign>

<#assign extraOptions>
  <#if isAssignedToMeLinkRendered?default(false)><@submitFormLink form="AssignContactHiddenForm" text="${uiLabelMap.OpentapsAssignToMe}" class="subMenuButton"/></#if>
  <#if isUnassignLinkRendered?default(false)><@submitFormLink form="UnassignContactHiddenForm" text="${uiLabelMap.OpentapsUnassign}" class="subMenuButton"/></#if>
  <#if hasUpdatePermission?exists><@displayLink href="updateContactForm?partyId=${partySummary.partyId}" text="${uiLabelMap.CommonEdit}" class="subMenuButton"/></#if>
  <#if isDeactivateLinkRendered?default(false)><@submitFormLinkConfirm form="DeactivateContactHiddenForm" text="${uiLabelMap.CrmDeactivateContact}" class="subMenuButtonDangerous" /></#if>
</#assign>

<@frameSectionHeader title=frameTitle extra=extraOptions/>
