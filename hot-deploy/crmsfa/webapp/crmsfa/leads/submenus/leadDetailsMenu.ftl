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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<#-- NOTE: the subMenuBar DIV has been compressed into one line due to layout issues temporarily until a good CSS fix is found -->

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
    <#assign update_options = update_options + "<a class='subMenuButton' href='updateLeadStatus?partyId="+partySummary.partyId+"&statusId=PTYLEAD_QUALIFIED'>"+uiLabelMap.CrmQualifyLead+"</a>"  />
  </#if>

  <#-- a lead can only be converted if it has already been qualified -->
  <#if (partySummary.statusId?exists) && (partySummary.statusId == 'PTYLEAD_QUALIFIED')>
    <#assign update_options = update_options + "<a class='subMenuButton' href='convertLeadForm?partyId="+partySummary.partyId+"'>"+uiLabelMap.CrmConvertLead+"</a>"  />
  </#if>

  <#assign update_options = update_options + "<a class='subMenuButton' href='updateLeadForm?partyId="+partySummary.partyId+"'>"+uiLabelMap.CommonEdit+"</a>" />

  <#if hasDeletePermission?exists>
    <#assign update_options = update_options + "<a class='subMenuButtonDangerous' href='deleteLead?leadPartyId=" + partySummary.partyId + "'>" + uiLabelMap.CommonDelete + "</a>"  />
  </#if>

</#if>
</#if>


<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmLead}</div>
    <div class="subMenuBar">${converted_options?if_exists}${create_option?if_exists}${update_options?if_exists}</div>
</div>
