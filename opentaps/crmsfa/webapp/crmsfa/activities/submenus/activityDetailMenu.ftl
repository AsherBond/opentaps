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

<#assign dateFormat = StringUtil.wrapString(Static["org.ofbiz.base.util.UtilDateTime"].getJsDateTimeFormat(Static["org.ofbiz.base.util.UtilDateTime"].getDateFormat(locale)))/>

<script type="text/javascript">
/*<![CDATA[*/
function updateTime() {
    var now = new Date();

    if (document.startActivityForm) {
        var startDate = document.startActivityForm.actualStartDate_c_date;
        var startHour = document.startActivityForm.actualStartDate_c_hour;
        var startMinutes = document.startActivityForm.actualStartDate_c_minutes;
        var startAmPm = document.startActivityForm.actualStartDate_c_ampm;

        startDate.value = opentaps.formatDate(now, '${dateFormat}');
        startHour.value = now.getHours() > 12 ? now.getHours() - 12 : now.getHours();
        startMinutes.value = now.getMinutes();
        startAmPm.value = now.getHours() > 12 ? 'PM' : 'AM';
    }
    
    if (document.endActivityForm) {
        var finishDate = document.endActivityForm.actualCompletionDate_c_date;
        var finishHour = document.endActivityForm.actualCompletionDate_c_hour;
        var finishMinutes = document.endActivityForm.actualCompletionDate_c_minutes;
        var finishAmPm = document.endActivityForm.actualCompletionDate_c_ampm;

        finishDate.value = opentaps.formatDate(now, '${dateFormat}');
        finishHour.value = now.getHours() > 12 ? now.getHours() - 12 : now.getHours();
        finishMinutes.value = now.getMinutes();
        finishAmPm.value = now.getHours() > 12 ? 'PM' : 'AM';
    }
}

opentaps.addOnLoad(setInterval('updateTime()', 1000*60 /*1 min*/));
/*]]>*/
</script>

<#assign activityTypeLabel = uiLabelMap.CrmEvent>
<#assign updateTarget = "updateEventForm"/>
<#if workEffort.workEffortTypeId == "TASK">
<#assign activityTypeLabel = uiLabelMap.CrmTask>
<#assign updateTarget = "updateTaskForm"/>
</#if>

<@form name="cancelActivityForm" url="updateActivityWithoutAssoc" workEffortId=workEffort.workEffortId currentStatusId="${workEffort.workEffortTypeId}_CANCELLED"/>

<#assign extraOptions>
  <#if true == (hasUpdatePermission)!false>
    <a class="subMenuButton" href="${updateTarget}?${activityValueParams}">${uiLabelMap.CommonEdit}</a><@submitFormLink form="cancelActivityForm" text=uiLabelMap.CommonCancel/>
  </#if>
</#assign>

<@frameSectionHeader title=activityTypeLabel extra=extraOptions />
