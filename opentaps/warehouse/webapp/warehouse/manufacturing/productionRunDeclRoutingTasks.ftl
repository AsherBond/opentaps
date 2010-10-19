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

<#-- these forms need to be out here, because we cannot nest them inside the ListProductionRunDeclRoutingTasks form below us -->
<#list productionRunRoutingTasks as productionRunRoutingTask>
 <@form name="changeProductionRunTaskStatusForm_${productionRunRoutingTask.workEffortParentId}_${productionRunRoutingTask.workEffortId}" url="changeProductionRunTaskStatus" workEffortId="${productionRunRoutingTask.workEffortId}" productionRunId="${productionRunRoutingTask.workEffortParentId}"/>
</#list>

<form name="ListProductionRunDeclRoutingTasks" action="<@ofbizUrl>ProductionRunDeclaration?productionRunId=${productionRunId}</@ofbizUrl>" method="post">
    <table class="listTable" cellspacing="0" style="width:100%">
        <tr class="listTableHeader">
            <td>${uiLabelMap.CommonSequenceNum}</td>
            <td>${uiLabelMap.ManufacturingTaskName}</td>
            <td>${uiLabelMap.CommonStatus}</td>
            <td>${uiLabelMap.ManufacturingMachine}</td>
            <td>${uiLabelMap.ManufacturingEstimatedCompletionDate}</td>
            <td>${uiLabelMap.ManufacturingTaskActualMilliSeconds}</td>
            <td>${uiLabelMap.ManufacturingQuantityProduced}</td>
            <td>&nbsp;</td>
        </tr>
        <#list productionRunRoutingTasks as productionRunRoutingTask>
                <tr class="${tableRowClass(productionRunRoutingTask_index)}">
                <@displayCell text=productionRunRoutingTask.priority/>
                <@displayCell text="${productionRunRoutingTask.workEffortName} [${productionRunRoutingTask.workEffortId}]"/>
                <@displayCell text=productionRunRoutingTask.statusItem?default({}).description?if_exists/>
                <@displayCell text=productionRunRoutingTask.fixedAssetId/>
                <@displayDateCell date=productionRunRoutingTask.estimatedCompletionDate/>
                <#assign totalTime = productionRunRoutingTask.actualMilliSeconds?default(0) + productionRunRoutingTask.actualSetupMillis?default(0)/>
                <@displayCell text=totalTime/>
                <@displayCell text=productionRunRoutingTask.quantityProduced/>
                <td class="tabletext" style="text-align:right">
                  <#if "PRUN_OUTSRC_PURCH" != productionRunRoutingTask.workEffortGoodStdTypeId && startTaskId?default("") = productionRunRoutingTask.workEffortId>`
                    <p><@submitFormLink form="changeProductionRunTaskStatusForm_${productionRunRoutingTask.workEffortParentId}_${productionRunRoutingTask.workEffortId}" text=uiLabelMap.ManufacturingStartProductionRunTask /></p>
                  </#if>
                  <#if "PRUN_OUTSRC_PURCH" != productionRunRoutingTask.workEffortGoodStdTypeId && "PRUN_RUNNING" == productionRunRoutingTask.currentStatusId>
                    <p><@displayLink href="ProductionRunDeclaration?actionForm=EditRoutingTask&amp;routingTaskId=${productionRunRoutingTask.workEffortId}&amp;productionRunId=${productionRunRoutingTask.workEffortParentId}" text=uiLabelMap.ManufacturingDeclareProductionRunTask class="buttontext"/></p>
                  </#if>
                  <#if "PRUN_OUTSRC_PURCH" != productionRunRoutingTask.workEffortGoodStdTypeId && completeTaskId == productionRunRoutingTask.workEffortId>
                    <p><@submitFormLink form="changeProductionRunTaskStatusForm_${productionRunRoutingTask.workEffortParentId}_${productionRunRoutingTask.workEffortId}" text=uiLabelMap.ManufacturingCompleteProductionRunTask /></p>
                  </#if>
                </td>
            </tr>
            <#if productionRunRoutingTask.timeComment?exists>
              <tr class="${tableRowClass(productionRunRoutingTask_index)}">
                <td></td>
                <td colspan="6">
                  ${productionRunRoutingTask.timeComment}
                </td>
              </tr>
            </#if>
        </#list>
    </table>
</form>
