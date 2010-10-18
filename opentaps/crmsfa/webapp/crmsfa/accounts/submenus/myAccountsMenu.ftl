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

<#if viewPreferences.get("MY_OR_TEAM_ACCOUNTS")?default("TEAM_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyAccounts />
  <#assign prefValue = "TEAM_VALUES" />
  <#assign prefButtonLabel = uiLabelMap.CrmTeamAccounts />
<#else> 
  <#assign title = uiLabelMap.CrmTeamAccounts />
  <#assign prefValue = "MY_VALUES"/>
  <#assign prefButtonLabel = uiLabelMap.CrmMyAccounts />
</#if>

<@form name="MyAccountsPrefChangeForm" url="setViewPreference" viewPrefValue="${prefValue}" viewPrefTypeId="MY_OR_TEAM_ACCOUNTS" donePage="myAccounts"/>

<#assign extraOptions>
  <@submitFormLink form="MyAccountsPrefChangeForm" text="${prefButtonLabel}" />
</#assign>

<@frameSectionTitleBar title=title?if_exists titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_${sectionName?if_exists}" extra=extraOptions/>
