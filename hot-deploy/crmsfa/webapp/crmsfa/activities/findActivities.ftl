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
<#-- Copyright (c) 2005-2008 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign activityStatuses = {"_SCHEDULED":uiLabelMap.CrmActivityScheduled, "_STARTED":uiLabelMap.CrmActivityStarted, "_ON_HOLD":uiLabelMap.CrmActivityOnHold, "_COMPLETED":uiLabelMap.CrmActivityCompleted, "_CANCELLED":uiLabelMap.CrmActivityCancelled } />

<div class="subSectionBlock">
    <form name="FindActivitiesForm" method="post" action="<@ofbizUrl>activitiesMain</@ofbizUrl>">
        <table class="twoColumnForm">
            <@inputTextRow name="workEffortName" title=uiLabelMap.CrmActivityName/>
            <@inputSelectHashRow name="currentStatusId" title=uiLabelMap.CommonStatus hash=activityStatuses required=false/>
            <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFromDate default=fromDate?if_exists/>
            <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThruDate default=thruDate?if_exists/>
            <@inputSubmitRow title=uiLabelMap.CrmFindActivities/>
        </table>
    </form>
</div>