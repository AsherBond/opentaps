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

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign status = productionRun.currentStatusId/>
<#if opentapsProductionRun.isAssembly()>
  <#assign title = uiLabelMap.ManufacturingProductionRunId>
<#elseif opentapsProductionRun.isDisassembly()>
  <#assign title = uiLabelMap.WarehouseReverseAssembly>
</#if>

<#assign printButton><a class="subMenuButton" href="<@ofbizUrl>PrintProductionRun.pdf?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.CommonPrint}</a></#assign>
<#assign scheduleButton><a class="subMenuButton" href="<@ofbizUrl>scheduleProductionRun?productionRunId=${productionRunId}&amp;statusId=PRUN_SCHEDULED</@ofbizUrl>">${uiLabelMap.ManufacturingSchedule}</a></#assign>
<#assign confirmButton><a class="subMenuButton" href="<@ofbizUrl>changeProductionRunStatusToPrinted?productionRunId=${productionRunId}</@ofbizUrl>">${uiLabelMap.ManufacturingConfirmProductionRun}</a></#assign>
<#assign cancelButton><@inputConfirm class="subMenuButtonDangerous" title=uiLabelMap.ManufacturingCancel href="cancelProductionRun?productionRunId=${productionRunId}"/></#assign>
<#assign closeButton><@inputConfirm class="subMenuButtonDangerous" title=uiLabelMap.WarehouseCloseProductionRun href="changeProductionRunStatusToClosed?productionRunId=${productionRunId}"/></#assign>
<#assign revertButton><@inputConfirm class="subMenuButtonDangerous" title=uiLabelMap.WarehouseManufacturingRevert href="revertProductionRun?productionRunId=${productionRunId}"/></#assign>

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
