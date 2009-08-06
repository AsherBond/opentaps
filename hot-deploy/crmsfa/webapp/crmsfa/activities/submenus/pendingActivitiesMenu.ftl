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

<#-- This is re-used everywhere for pending activities so it is one standard page.  Whichever page calls it should put in the activityValueParams, be they party, case, or opportunity ids -->
<a name="ListPendingActivities"></a>
<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmPendingActivities}</div><#if hasNewActivityPermission?exists><div class="subMenuBar"><a class="subMenuButton" href="createEventForm?${activityValueParams}&workEffortTypeId=EVENT">${uiLabelMap.CrmCreateNewEvent}</a><a class="subMenuButton" href="createTaskForm?${activityValueParams}&workEffortTypeId=TASK">${uiLabelMap.CrmCreateNewTask}</a></div>
    </#if>
</div>
