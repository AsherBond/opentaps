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

<#if viewPreferences.get("MY_OR_TEAM_OPPS")?default("TEAM_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyOpportunities />
  <#assign prefValue = "TEAM_VALUES"/>
  <#assign prefButtonLabel = uiLabelMap.CrmTeamOpportunities />
<#else> 
  <#assign title = uiLabelMap.CrmTeamOpportunities />
  <#assign prefValue = "MY_VALUES"/>
  <#assign prefButtonLabel = uiLabelMap.CrmMyOpportunities />
</#if>

<@form name="PrefChangeForm" url="setViewPreference" viewPrefValue="${prefValue}" donePage="myOpportunities" viewPrefTypeId="MY_OR_TEAM_OPPS"/>

<#assign extraOptions>
  <@submitFormLink form="PrefChangeForm" text="${prefButtonLabel}" />
</#assign>

<@frameSectionHeader title=title! extra=extraOptions/>
