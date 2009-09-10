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

<#assign dayLink = "<a class='subMenuButton' href='"+calendarTarget+"?calendarView=day'>" + uiLabelMap.CommonDay + "</a>"/>
<#assign weekLink = "<a class='subMenuButton' href='"+calendarTarget+"?calendarView=week'>" + uiLabelMap.CommonWeek + "</a>"/>
<#assign monthLink = "<a class='subMenuButton' href='"+calendarTarget+"?calendarView=month'>" + uiLabelMap.CommonMonth + "</a>"/>

<#-- default title -->
<#assign title = uiLabelMap.CrmMyCalendar />

<#-- show link to change between my calendar and team calendar -->
<#if hasTeamCalviewPermission>
  <form name="setCalendarViewPref" method="post" action="<@ofbizUrl>setViewPreference</@ofbizUrl>">
    <#if viewPreferences.get("MY_OR_TEAM_CALENDAR")?default("MY_VALUES") == "MY_VALUES">
      <@inputHidden name="viewPrefValue" value="TEAM_VALUES" />
      <#assign prefChange = "<a class='subMenuButton' href='javascript:document.setCalendarViewPref.submit()'>" + uiLabelMap.CrmTeamCalendar + "</a>" />
    <#else> 
      <#assign title = uiLabelMap.CrmTeamCalendar />
      <@inputHidden name="viewPrefValue" value="MY_VALUES" />
      <#assign prefChange = "<a class='subMenuButton' href='javascript:document.setCalendarViewPref.submit()'>" + uiLabelMap.CrmMyCalendar + "</a>" />
    </#if>
    <@inputHidden name="donePage" value=calendarTarget />
    <@inputHidden name="viewPrefTypeId" value="MY_OR_TEAM_CALENDAR" />
    <@inputHidden name="calendarView" value=parameters.calendarView?default("day") />
  </form>
</#if>

<#assign headerContent>${prefChange?if_exists}${dayLink}${weekLink}${monthLink}</#assign>
<@flexAreaClassic targetId="myHomeCalendar" title=title save=true defaultState="open" style="border:none; margin:0; padding:0" headerContent=headerContent>
  ${screens.render("component://crmsfa/widget/crmsfa/screens/myhome/MyHomeScreens.xml#Calendar")}
</@flexAreaClassic>
