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
<#assign prefChangeAction = "">
<#if viewPreferences.get("MY_OR_TEAM_CASES")?default("TEAM_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyCases />
  <@form name="setTeamCasesViewPref" url="setViewPreference" donePage="myCases" viewPrefTypeId="MY_OR_TEAM_CASES" viewPrefValue="TEAM_VALUES" />
  <#assign prefChangeAction><@submitFormLink form="setTeamCasesViewPref" text=uiLabelMap.CrmTeamCases class="subMenuButton" /></#assign>
<#else> 
  <#assign title = uiLabelMap.CrmTeamCases />
  <@form name="setMyCasesViewPref" url="setViewPreference" donePage="myCases" viewPrefTypeId="MY_OR_TEAM_CASES" viewPrefValue="MY_VALUES" />
  <#assign prefChangeAction><@submitFormLink form="setMyCasesViewPref" text=uiLabelMap.CrmMyCases class="subMenuButton" /></#assign>
</#if>
  </form>

<@sectionHeader title=title?if_exists>
  ${prefChangeAction?if_exists}
</@sectionHeader>
