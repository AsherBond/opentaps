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

<#assign prefParams = "donePage=myCases&viewPrefTypeId=MY_OR_TEAM_CASES " />
<#if viewPreferences.get("MY_OR_TEAM_CASES")?default("TEAM_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyCases />
  <#assign prefChange = "<a class='subMenuButton' href='setViewPreference?viewPrefValue=TEAM_VALUES&"+prefParams+"'>" + uiLabelMap.CrmTeamCases + "</a>" />
<#else> 
  <#assign title = uiLabelMap.CrmTeamCases />
  <#assign prefChange = "<a class='subMenuButton' href='setViewPreference?viewPrefValue=MY_VALUES&"+prefParams+"'>" + uiLabelMap.CrmMyCases + "</a>" />
</#if>

<div class="subSectionHeader">
    <div class="subSectionTitle">${title?if_exists}</div>
    <div class="subMenuBar">${prefChange?if_exists}</div>
</div>
