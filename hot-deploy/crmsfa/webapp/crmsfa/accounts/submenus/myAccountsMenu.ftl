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

<div class="subSectionHeader  sectionHeaderTitle" id="sectionHeaderTitle_accounts" style="font: bold 14pt Verdana, Arial, Helvetica, sans-serif; padding-top: 5px; padding-bottom: 0px; height: 1.3em; border-bottom: 1px solid #AAAAAA;">
    <div class="subSectionTitle">${title?if_exists}</div>
    <div class="subMenuBar"><@submitFormLink form="MyAccountsPrefChangeForm" text="${prefButtonLabel}" /></div>
</div>
