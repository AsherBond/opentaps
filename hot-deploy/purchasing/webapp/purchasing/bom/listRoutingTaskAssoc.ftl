<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<#if allRoutingTasks?has_content>
  <table class="listTable">
    <tr class="listTableHeader">
      <@displayCell text=uiLabelMap.ManufacturingTaskName />
      <@displayCell text=uiLabelMap.CommonSequenceNum />
      <@displayCell text=uiLabelMap.CommonFromDate />
      <@displayCell text=uiLabelMap.CommonThruDate />
      <@displayCell text=uiLabelMap.ManufacturingTaskEstimatedSetupMillis />
      <@displayCell text=uiLabelMap.ManufacturingTaskEstimatedMilliSeconds />
      <td>&nbsp;</td>
    </tr>
    <#list allRoutingTasks as routingTask>
      <tr class="${tableRowClass(routingTask_index)}">
        <@displayLinkCell text="[${routingTask.workEffortIdTo}] ${routingTask.workEffortToName}" href="EditRoutingTask?workEffortId=${routingTask.workEffortIdTo}" />
        <@displayCell text=routingTask.sequenceNum />
        <@displayCell text=routingTask.fromDate />
        <@displayCell text=routingTask.thruDate />
        <@displayCell text=routingTask.workEffortToSetup />
        <@displayCell text=routingTask.workEffortToRun />
        <@displayLinkCell text=uiLabelMap.CommonDelete href="RemoveRoutingTaskAssoc?workEffortId=${routingTask.workEffortIdFrom}&workEffortIdFrom=${routingTask.workEffortIdFrom}&workEffortIdTo=${routingTask.workEffortIdTo}&fromDate=${routingTask.fromDate}&workEffortAssocTypeId=${routingTask.workEffortAssocTypeId}" class="buttontext"/>
      </tr>
    </#list>
  </table>
</#if>
