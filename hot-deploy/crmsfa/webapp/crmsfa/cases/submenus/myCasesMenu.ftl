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

<div class="subSectionHeader">
    <div class="subSectionTitle">${title?if_exists}</div>
    <div class="subMenuBar">${prefChangeAction?if_exists}</div>
</div>
