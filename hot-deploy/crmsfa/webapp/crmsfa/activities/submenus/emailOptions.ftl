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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if communicationEvent?exists>
    <div class="subSectionHeader">
      <div class="subMenuBar">
        <a class="subMenuButton" href="<@ofbizUrl>writeEmail?workEffortId=${workEffort.workEffortId}&amp;communicationEventId=${communicationEvent.communicationEventId}&amp;action=reply</@ofbizUrl>">${uiLabelMap.PartyReply}</a>
        <a class="subMenuButton" href="<@ofbizUrl>writeEmail?workEffortId=${workEffort.workEffortId}&amp;communicationEventId=${communicationEvent.communicationEventId}&amp;action=forward</@ofbizUrl>">${uiLabelMap.OpentapsEmailForward}</a>
        <#if hasUpdatePermission == true>
          <#if "TASK_COMPLETED" != workEffort.currentStatusId?default("")>
            <a class="subMenuButton" href="<@ofbizUrl>updateEmailActivity?workEffortId=${workEffort.workEffortId}&amp;currentStatusId=TASK_COMPLETED</@ofbizUrl>">${uiLabelMap.OpentapsComplete}</a>
          </#if>
          <@inputConfirm class="subMenuButtonDangerous" title=uiLabelMap.CrmDeleteEmail href="deleteActivityEmail?communicationEventId=${communicationEvent.communicationEventId}&amp;workEffortId=${workEffort.workEffortId}&amp;delContentDataResource=Y&amp;donePage=${parameters.fromPage?if_exists}" />
        </#if>
      </div>
    </div>
</#if>
