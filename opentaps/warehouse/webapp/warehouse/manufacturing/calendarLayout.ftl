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

<#assign currentView = (parameters.calendarView)?default("week")/>

<#assign headerContent><@displayLink href=calendarTarget+"?calendarView=day" text=uiLabelMap.CommonDay class="subMenuButton"/><@displayLink href=calendarTarget+"?calendarView=week" text=uiLabelMap.CommonWeek class="subMenuButton"/><@displayLink href=calendarTarget+"?calendarView=month" text=uiLabelMap.CommonMonth class="subMenuButton"/></#assign>
<#if facilityName?has_content>
    <#assign calendarTitle = facilityName?default("")/>
<#else>
    <#assign calendarTitle = parameters.facilityId?default("")/>
</#if>
<@flexAreaClassic targetId="manufacturingCalendar" title="${calendarTitle} ${uiLabelMap.ManufacturingCalendar}" save=true defaultState="open" style="border:none; margin:0; padding:0" headerContent=headerContent>
    <#if currentView == "day">
        ${screens.render("component://opentaps-common/widget/screens/calendar/CalendarScreens.xml#CalendarDayWidget")}
    <#elseif currentView == "week">
        ${screens.render("component://opentaps-common/widget/screens/calendar/CalendarScreens.xml#CalendarWeekWidget")}
    <#elseif currentView == "month">
        ${screens.render("component://opentaps-common/widget/screens/calendar/CalendarScreens.xml#CalendarMonthWidget")}
    </#if>
</@flexAreaClassic>