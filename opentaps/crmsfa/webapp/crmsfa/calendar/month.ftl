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
 *@created    May 19 2003
 *@author     Eric.Barbier@nereide.biz (migration to uiLabelMap)
 *@version    1.0
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<table width="100%" border="0" cellspacing="0" cellpadding="0" class="monthheadertable">
  <tr>
	<td width="100%" class="monthheadertext">${start?date?string("MMMM yyyy")?cap_first}</td>
    <td nowrap class="previousnextmiddle"><a href='<@ofbizUrl>${calendarTarget}?calendarView=month&start=${prev.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.CommonPrevious}</a> | <a href='<@ofbizUrl>${calendarTarget}?calendarView=month&start=${now.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.CommonCurrent}</a> | <a href='<@ofbizUrl>${calendarTarget}?calendarView=month&start=${next.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="previousnext">${uiLabelMap.CommonNext}</a></td>
  </tr>
</table>

<#if periods?has_content> 
<table width="100%" cellspacing="1" border="0" cellpadding="1" class="scheduler">
  <tr class="bg">
    <td width="1%" class="monthdayheader">&nbsp;<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="88"></td>
    <#list periods as day>
    <td width="14%" class="monthdayheader">${day.start?date?string("EEEE")?cap_first}<br/>
      <img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1" width="1"></td>
    <#if (day_index > 5)><#break></#if>
  	</#list>
  </tr>
  <#list periods as period>
  <#assign indexMod7 = period_index % 7>
  <#if indexMod7 = 0>
  <tr class="bg">
    <td valign="top" height="60" nowrap class="monthweekheader"><a href='<@ofbizUrl>${calendarTarget}?calendarView=week&start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="monthweeknumber">${uiLabelMap.CommonWeek} ${period.start?date?string("w")}</a></td>
  </#if>			  
    <td valign="top">
      <table width="100%" cellspacing="0" cellpadding="0" border="0">			
        <tr>
          <td nowrap class="monthdaynumber"><a href='<@ofbizUrl>${calendarTarget}?calendarView=day&start=${period.start.time?string("#")}<#if eventsParam?has_content>&${eventsParam}</#if></@ofbizUrl>' class="monthdaynumber">${period.start?date?string("d")?cap_first}</a></td>
          <td valign="top" align="right">
          <#assign startWithHour = Static["org.ofbiz.base.util.UtilDateTime"].stringToTimeStamp(period.start?string("yyyy-MM-dd") + " " + configProperties.get("crmsfa.calendar.startHour") + ":00:00.0", "yyyy-MM-dd HH:mm:ss.S", timeZone, locale)/>
          <a href="<@ofbizUrl>createEventForm?estimatedStartDate=${getLocalizedDate(startWithHour, "DATE_TIME", true)}</@ofbizUrl>"><img class="imageLinkBorderless" src="<@ofbizContentUrl>${configProperties.get("crmsfa.theme.icon.button.createEvent")}</@ofbizContentUrl>"/></a>
          </td>
        </tr>			
      </table>
      <#list period.calendarEntries as calEntry>
      <table width="100%" cellspacing="0" cellpadding="0" border="0">			
        <tr width="100%">
          <td class='monthschedulerentry' width="100%" valign='top'>
		    <#if (calEntry.workEffort.estimatedStartDate.compareTo(period.start)  <= 0 && calEntry.workEffort.estimatedCompletionDate.compareTo(period.end) >= 0)>
		    ${uiLabelMap.CommonAllDay}
		    <#elseif calEntry.workEffort.estimatedStartDate.before(period.start)>
		    ${uiLabelMap.CommonUntil} ${calEntry.workEffort.estimatedCompletionDate?time?string.short}
			<#elseif calEntry.workEffort.estimatedCompletionDate.after(period.end)>
		     ${uiLabelMap.CommonFrom} ${calEntry.workEffort.estimatedStartDate?time?string.short}
		    <#else>
		    ${calEntry.workEffort.estimatedStartDate?time?string.short}-${calEntry.workEffort.estimatedCompletionDate?time?string.short}
		    </#if>
		    <br/>
		    <a href="<@ofbizUrl>viewActivity?workEffortId=${calEntry.workEffort.workEffortId}</@ofbizUrl>" class="event">${calEntry.workEffort.workEffortName?default("Undefined")}</a>&nbsp;
          </td>
        </tr>			
      </table>
     </#list>
     <#-- TODO: put links for adding events to this date here -->
    </td>
    <#if !period_has_next && indexMod7 != 6>			  
    <td colspan='${6 - (indexMod7)}'>&nbsp;</td>
    </#if>			
  <#if indexMod7 = 6 || !period_has_next>
  </tr>
  </#if>
  </#list>
</table>

<#else> 
<p>${uiLabelMap.OpentapsError_Internal}!</p>
</#if>
