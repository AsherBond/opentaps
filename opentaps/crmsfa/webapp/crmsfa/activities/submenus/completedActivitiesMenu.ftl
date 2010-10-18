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

<#assign extraOptions>
  <#if hasNewActivityPermission?exists>
    <a class="subMenuButton" href="logTaskForm?${activityValueParams}&workEffortPurposeTypeId=WEPT_TASK_PHONE_CALL">${uiLabelMap.CrmLogCall}</a>
    <#-- this duration parameter causes the duration on the next form to be 0, which is logical for old emails you're copying in -->
    <a class="subMenuButton" href="logTaskForm?${activityValueParams}&workEffortPurposeTypeId=WEPT_TASK_EMAIL&duration=0%3A00">${uiLabelMap.CrmLogEmail}</a>
  </#if>
</#assign>

<#-- This is re-used everywhere for pending activities so it is one standard page.  Whichever page calls it should put in the activityValueParams, be they party, case, or opportunity ids -->
<a name="ListCompletedActivities"></a>
<@frameSectionHeader title=uiLabelMap.CrmCompletedActivities extra=extraOptions/>
