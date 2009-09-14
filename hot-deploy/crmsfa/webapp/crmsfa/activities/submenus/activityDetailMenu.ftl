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

<#assign dateFormat = StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))/>

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

<div class="subSectionHeader">
    <div class="subSectionTitle">${activityTypeLabel}</div>
    <#if hasUpdatePermission == true>
    <div class="subMenuBar"><a class="subMenuButton" href="${updateTarget}?${activityValueParams}">${uiLabelMap.CommonEdit}</a><a class="subMenuButton" href="updateActivityWithoutAssoc?workEffortId=${workEffort.workEffortId}&${cancelActivityParams?if_exists}">${uiLabelMap.CommonCancel}</a></div>
    </#if>
</div>
