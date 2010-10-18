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

<#-- This is re-used everywhere for pending activities so it is one standard page.  Whichever page calls it should put in the activityValueParams, be they party, case, or opportunity ids -->
<a name="ListPendingActivities"></a>

<#assign extraOptions>
  <#if hasNewActivityPermission?exists>
    <a class="subMenuButton" href="createEventForm?${activityValueParams}&workEffortTypeId=EVENT">${uiLabelMap.CrmCreateNewEvent}</a><a class="subMenuButton" href="createTaskForm?${activityValueParams}&workEffortTypeId=TASK">${uiLabelMap.CrmCreateNewTask}</a>
  </#if>
</#assign>

<@frameSectionHeader title=uiLabelMap.CrmPendingActivities extra=extraOptions/>
