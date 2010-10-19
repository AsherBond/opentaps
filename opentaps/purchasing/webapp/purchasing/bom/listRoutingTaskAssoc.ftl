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
        <@form name="removeRoutingTaskAssocForm_${routingTask_index}" url="RemoveRoutingTaskAssoc" workEffortId="${routingTask.workEffortIdFrom}" workEffortIdFrom="${routingTask.workEffortIdFrom}" workEffortIdTo="${routingTask.workEffortIdTo}" fromDate="${routingTask.fromDate}" workEffortAssocTypeId="${routingTask.workEffortAssocTypeId}"/>
        <td>
        <@submitFormLink form="removeRoutingTaskAssocForm_${routingTask_index}" text="${uiLabelMap.CommonDelete}" class="buttontext"/>
        </td>
      </tr>
    </#list>
  </table>
</#if>
