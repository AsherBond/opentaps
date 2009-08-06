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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@paginate name="pendingActivities" list=pendingActivities>
<#noparse>
<@navigationHeader title=uiLabelMap.CrmPendingActivities />
<table class="listTable">
    <tr class="listTableHeader">
        <@headerCell title=uiLabelMap.CommonType orderBy="workEffortTypeId" />
        <@headerCell title=uiLabelMap.CrmPurpose orderBy="workEffortPurposeTypeId" />
        <@headerCell title=uiLabelMap.CrmActivity orderBy="workEffortName" />
        <@headerCell title=uiLabelMap.CommonStatus orderBy="currentStatusId" />
        <@headerCell title=uiLabelMap.CrmActivityScheduledDate orderBy="estimatedStartDate" />
        <@headerCell title=uiLabelMap.CrmActivityDueDate orderBy="estimatedCompletionDate" />
    </tr>
    <#list pageRows as activity>
        <#assign status = activity.getRelatedOneCache("CurrentStatusItem")>
        <#assign type = activity.getRelatedOneCache("WorkEffortType")>
        <#assign purpose = activity.getRelatedOneCache("WorkEffortPurposeType")?if_exists>
        <#assign updated = Static["org.opentaps.common.workeffort.WorkEffortHelper"].isUpdatedSinceLastView(activity, userLogin.getString("userLoginId"))>
        <#assign class = "activity_${activity.currentStatusId}" + updated?string(" activity_updated","")>

        <tr class="${class}">
            <@displayCell text=type.description />
            <@displayCell text=purpose.description?if_exists />
            <@displayLinkCell href="viewActivity?workEffortId=${activity.workEffortId}" text="${activity.workEffortName} (${activity.workEffortId})" class="linktext"/>
            <@displayCell text=status.description />
            <@displayDateCell date=activity.estimatedStartDate />
            <@displayDateCell date=activity.estimatedCompletionDate />
        </tr>
    </#list>
</table>
</#noparse>
</@paginate>