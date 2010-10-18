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
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Jacopo Cappellato (jacopo.cappellato@sastau.it)
-->
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<div class="tabContainer">
    <a href="<@ofbizUrl>day<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.day?default(unselectedClassName)}">${uiLabelMap.CommonDayView}</a>
    <a href="<@ofbizUrl>week<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.week?default(unselectedClassName)}">${uiLabelMap.CommonWeekView}</a>
    <a href="<@ofbizUrl>month<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.month?default(unselectedClassName)}">${uiLabelMap.CommonMonthView}</a>
    <a href="<@ofbizUrl>upcoming<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.upcoming?default(unselectedClassName)}">${uiLabelMap.CommonUpcomingEvents}</a>
</div>

<div>
    <a href="<@ofbizUrl>EditActivity?workEffortTypeId=EVENT&currentStatusId=EVENT_SCHEDULED</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNewEvent}</a>
</div>
<br/>
