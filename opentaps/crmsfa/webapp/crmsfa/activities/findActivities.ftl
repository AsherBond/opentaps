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