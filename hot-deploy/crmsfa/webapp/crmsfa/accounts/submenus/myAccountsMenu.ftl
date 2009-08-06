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

<#assign prefParams = "donePage=myAccounts&viewPrefTypeId=MY_OR_TEAM_ACCOUNTS" />
<#if viewPreferences.get("MY_OR_TEAM_ACCOUNTS")?default("TEAM_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyAccounts />
  <#assign prefChange = "<a class='subMenuButton' href='setViewPreference?viewPrefValue=TEAM_VALUES&"+prefParams+"'>" + uiLabelMap.CrmTeamAccounts + "</a>" />
<#else> 
  <#assign title = uiLabelMap.CrmTeamAccounts />
  <#assign prefChange = "<a class='subMenuButton' href='setViewPreference?viewPrefValue=MY_VALUES&"+prefParams+"'>" + uiLabelMap.CrmMyAccounts + "</a>" />
</#if>

<div class="subSectionHeader  sectionHeaderTitle" id="sectionHeaderTitle_accounts" style="font: bold 14pt Verdana, Arial, Helvetica, sans-serif; padding-top: 5px; padding-bottom: 0px; height: 1.3em; border-bottom: 1px solid #AAAAAA;">
    <div class="subSectionTitle">${title?if_exists}</div>
    <div class="subMenuBar">${prefChange?if_exists}</div>
</div>
