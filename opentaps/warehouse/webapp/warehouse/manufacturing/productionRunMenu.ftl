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

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign status = productionRun.currentStatusId/>
<#if opentapsProductionRun.isAssembly()>
  <#assign title = uiLabelMap.ManufacturingProductionRunId>
<#elseif opentapsProductionRun.isDisassembly()>
  <#assign title = uiLabelMap.WarehouseReverseAssembly>
</#if>

<#assign printButton><a class="subMenuButton" href="<@ofbizUrl>PrintProductionRun.pdf?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.CommonPrint}</a></#assign>

<@form name="scheduleProductionRunForm" url="scheduleProductionRun" productionRunId="${productionRunId}" statusId="PRUN_SCHEDULED"/>
<#assign scheduleButton><@submitFormLink form="scheduleProductionRunForm" text="${uiLabelMap.ManufacturingSchedule}"/></#assign>

<@form name="confirmProductionRunForm" url="changeProductionRunStatusToPrinted" productionRunId="${productionRunId}" />
<#assign confirmButton><@submitFormLink form="confirmProductionRunForm" text="${uiLabelMap.ManufacturingConfirmProductionRun}" /></#assign>

<@form name="cancelProductionRunForm" url="cancelProductionRun" productionRunId="${productionRunId}" />
<#assign cancelButton><@submitFormLinkConfirm form="cancelProductionRunForm" text="${uiLabelMap.ManufacturingCancel}"/></#assign>

<@form name="closeProductionRunForm" url="changeProductionRunStatusToClosed" productionRunId="${productionRunId}" />
<#assign closeButton><@submitFormLinkConfirm form="closeProductionRunForm" text=uiLabelMap.WarehouseCloseProductionRun /></#assign>

<@form name="revertProductionRunForm" url="revertProductionRun" productionRunId="${productionRunId}" />
<#assign revertButton><@submitFormLinkConfirm form="revertProductionRunForm" text=uiLabelMap.WarehouseManufacturingRevert /></#assign>

<#assign menuButtons = printButton />

<#if status == "PRUN_CREATED" || status == "PRUN_SCHEDULED">
  <#if status != "PRUN_SCHEDULED">
    <#assign menuButtons = menuButtons + scheduleButton />
  </#if>
  <#assign menuButtons = menuButtons + confirmButton + cancelButton />
<#else/>
  <#if status != "PRUN_CANCELLED" && status != "PRUN_COMPLETED" && status != "PRUN_CLOSED" && status != "PRUN_REVERTED">
    <#assign menuButtons = menuButtons + revertButton />
    <#if status != "PRUN_RUNNING">
      <#assign menuButtons = menuButtons + cancelButton />
    </#if>
  </#if>
  <#if status == "PRUN_COMPLETED">
    <#assign menuButtons = menuButtons + closeButton />
  </#if>
</#if>

<div class="subSectionHeader">
    <div class="subSectionTitle">${title}: ${productionRunId}</div>
    <div class="subMenuBar">${menuButtons}</div>
</div>
