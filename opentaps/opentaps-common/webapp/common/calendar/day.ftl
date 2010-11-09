`<#--
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

<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Johan Isacsson
 *@author     Eric.Barbier@nereide.biz (migration to uiLabelMap)
 *@created    May 19 2003
 *@version    1.0
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign prevLink = calendarTarget + "?calendarView=day&amp;start=" + prev.time?string("#")/>
<#assign todayLink = calendarTarget + "?calendarView=day&amp;start=" + now.time?string("#")/>
<#assign nextLink = calendarTarget + "?calendarView=day&amp;start=" + next.time?string("#")/>

<#-- Header table with the spelled-out date and the navigation bar -->
<table width="100%" border="0" cellspacing="0" cellpadding="0" class="monthheadertable">
    <tr>
      <td width="100%" class="monthheadertext">${start?date?string("EEEE")?cap_first} ${start?date?string.long}</td>
      <td nowrap="nowrap" class="previousnextmiddle"><a href="<@ofbizUrl>${prevLink}</@ofbizUrl>" class="previousnext">${uiLabelMap.CommonPrevious}</a> | <a href="<@ofbizUrl>${todayLink}</@ofbizUrl>" class="previousnext">${uiLabelMap.CommonToday}</a> | <a href="<@ofbizUrl>${nextLink}</@ofbizUrl>" class="previousnext">${uiLabelMap.CommonNext}</a></td>
    </tr>
</table>

<#if periods?has_content>

<#-- set the entry column width to full divided by the number of entries -->
<#assign entryWidth = 100/>
<#if (maxConcurrentEntries gt 1)>
  <#assign entryWidth = (100 / (maxConcurrentEntries))/>
</#if>

<#-- arrange the work efforts in columns with hours on the left column -->
<table width="100%" cellspacing="1" border="0" cellpadding="1" class="scheduler">

  <#assign thisHour = 0/>
  <#list periods as period>

    <#-- Decide whether to draw the row -->
    <#assign renderRow = true/>
    <#if (thisHour lt startHour)>
      <#if (period.calendarEntries.size() == 0)>
        <#assign renderRow = false/>
      </#if>
    </#if>
    <#if (thisHour gt endHour)>
      <#if (period.calendarEntries.size() == 0)>
        <#assign renderRow = false/>
      </#if>
    </#if>

    <#if renderRow == true>
      <tr>
        <#-- print the hour of this period -->
        <td valign="top" nowrap="nowrap" width="1%" class="monthweekheader"><span class="monthweeknumber">${period.start?time?string.short}</span>&nbsp;<a href="<@ofbizUrl>${createRequest}?estimatedStartDate=${getLocalizedDate(period.start, "DATE_TIME", true)}</@ofbizUrl>"><img class="imageLinkBorderless" src="<@ofbizContentUrl>/opentaps_images/openclipart.org/folder_16x16.png</@ofbizContentUrl>" alt="${uiLabelMap.CommonCreateNew}"/></a><br/></td>

        <#-- print each overlapping entry in cells from left to right -->
        <#list period.calendarEntries as calEntry>
          <#if calEntry.startOfPeriod>
            <#assign eventStart = calEntry.workEffort.estimatedStartDate!/>
            <#if calEntry.workEffort.actualStartDate?has_content>
              <#assign eventStart = calEntry.workEffort.actualStartDate!/>
            </#if>
            <#if eventStart?has_content>
              <#assign eventEnd = calEntry.workEffort.estimatedCompletionDate!/>
              <#if calEntry.workEffort.actualCompletionDate?has_content>
                <#assign eventEnd = calEntry.workEffort.actualCompletionDate!/>
              </#if>
              <#if eventEnd?has_content>
                <td class="schedulerentry" rowspan="${calEntry.periodSpan}" colspan="1" width="${entryWidth?string("#")}%" valign="top">
                  <#if (eventStart.compareTo(start)  <= 0 && eventEnd.compareTo(next) >= 0)>
                    ${uiLabelMap.CommonAllDay}
                  <#elseif eventStart.before(start)>
                    ${uiLabelMap.CommonUntil}${eventEnd?time?string.short}
                  <#elseif eventEnd.after(next)>
                    ${uiLabelMap.CommonFrom} ${eventStart?time?string.short}
                  <#else>
                    ${eventStart?time?string.short}-${eventEnd?time?string.short}
                  </#if>
                  <br/>
                  <a href="<@ofbizUrl>${viewRequest}?workEffortId=${calEntry.workEffort.workEffortId}</@ofbizUrl>" class="event">
                    ${calEntry.workEffort.workEffortName?default("Undefined")}
                  </a>
                  &nbsp;
                </td>
              </#if>
            </#if>
          </#if>
        </#list>

        <#-- Fill empty cells -->
        <#if period.calendarEntries?size lt maxConcurrentEntries>
          <#assign emptySlots = (maxConcurrentEntries - period.calendarEntries?size)/>
          <#list 1..emptySlots as num>
            <td width="${entryWidth?string("#")}%"  class="schedulerempty"><br/></td>
          </#list>
        </#if>

        <#-- if no entries, fill with an empty cell -->
        <#if maxConcurrentEntries == 0>
          <td width="${entryWidth?string("#")}" class="schedulerempty">&nbsp;</td>
        </#if>

      </tr>
    </#if>  <#-- end if renderRow == true-->

    <#assign thisHour = thisHour + 1/>
  </#list>

</table>
<#else>
    <p>${uiLabelMap.OpentapsError_CalendarFail}</p>
</#if>
