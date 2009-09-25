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
  
  <@form name="replyEmailAction"    url="writeEmail" workEffortId=workEffort.workEffortId communicationEventId=communicationEvent.communicationEventId action="reply" method="get"/>
  <@form name="forwardEmailAction"  url="writeEmail" workEffortId=workEffort.workEffortId communicationEventId=communicationEvent.communicationEventId action="forward" method="get"/>
  <@form name="completeEmailAction" url="updateEmailActivity" workEffortId=workEffort.workEffortId currentStatusId="TASK_COMPLETED" />
  <@form name="deleteEmailAction"   url="deleteActivityEmail" workEffortId=workEffort.workEffortId communicationEventId=communicationEvent.communicationEventId delContentDataResource="Y" donePage="${parameters.fromPage!}" />

  <div class="subSectionHeader">
    <div class="subMenuBar">
      <@submitFormLink form="replyEmailAction" text=uiLabelMap.PartyReply class="subMenuButton" />
      <@submitFormLink form="forwardEmailAction" text=uiLabelMap.OpentapsEmailForward class="subMenuButton" />
      <#if hasUpdatePermission == true>
        <#if "TASK_COMPLETED" != workEffort.currentStatusId?default("")>
          <@submitFormLink form="completeEmailAction" text=uiLabelMap.OpentapsComplete class="subMenuButton" />
        </#if>
        <@submitFormLinkConfirm form="deleteEmailAction" text=uiLabelMap.CrmDeleteEmail class="subMenuButtonDangerous" />
      </#if>
    </div>
  </div>
</#if>
