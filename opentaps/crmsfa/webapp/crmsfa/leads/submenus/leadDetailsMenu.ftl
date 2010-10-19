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

<#-- NOTE: the subMenuBar DIV has been compressed into one line due to layout issues temporarily until a good CSS fix is found -->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if (security.hasEntityPermission("CRMSFA_LEAD", "_CREATE", session)) && (partySummary?exists)>
  <#assign create_option = "<a class='subMenuButton' href='duplicateLeadForm?partyId="+partySummary.partyId+"'>"+uiLabelMap.CrmDuplicateLead+"</a>"  />
</#if>

<#-- if lead has been converted, then no operations should be done -->
<#if hasBeenConverted?exists>

  <#assign converted_options = "<span class='subSectionWarning'>${uiLabelMap.CrmLeadHasBeenConverted}</span>"  />
  <#assign converted_options = converted_options + "<a class='subMenuButton' href='viewContact?partyId="+parameters.partyId+"'>"+uiLabelMap.CrmViewAsContact+"</a>"  />

<#-- otherwise check for options if update permission exists -->
<#else><#if hasUpdatePermission?exists>
  <#assign update_options = ""/>

  <#-- a lead can be qualified it has already been assigned -->
  <#if (partySummary.statusId?exists) && (partySummary.statusId == 'PTYLEAD_ASSIGNED')>
    <form name="qualifyLeadForm" method="post" action="<@ofbizUrl>updateLeadStatus</@ofbizUrl>"/>
      <@inputHidden name="partyId" value="${partySummary.partyId}"/>
      <@inputHidden name="statusId" value="PTYLEAD_QUALIFIED"/>
    </form>
    <#assign update_options = update_options + "<a class='subMenuButton' href='javascript:document.qualifyLeadForm.submit()'>" + uiLabelMap.CrmQualifyLead + "</a>"  />
  </#if>

  <#-- a lead can only be converted if it has already been qualified -->
  <#if (partySummary.statusId?exists) && (partySummary.statusId == 'PTYLEAD_QUALIFIED')>
    <#assign update_options = update_options + "<a class='subMenuButton' href='convertLeadForm?partyId="+partySummary.partyId+"'>"+uiLabelMap.CrmConvertLead+"</a>"  />
  </#if>

  <#assign update_options = update_options + "<a class='subMenuButton' href='updateLeadForm?partyId="+partySummary.partyId+"'>"+uiLabelMap.CommonEdit+"</a>" />

  <#if hasDeletePermission?exists>
    <form name="deleteLeadForm" method="post" action="<@ofbizUrl>deleteLead</@ofbizUrl>"/>
      <@inputHidden name="leadPartyId" value="${partySummary.partyId}"/>
    </form>
    <#assign update_options = update_options + "<a class='subMenuButtonDangerous' href='javascript:document.deleteLeadForm.submit()'>" + uiLabelMap.CommonDelete + "</a>"  />
  </#if>

</#if>
</#if>

<@frameSectionHeader title="${uiLabelMap.CrmLead}" extra="${converted_options?if_exists}${create_option?if_exists}${update_options?if_exists}" />
