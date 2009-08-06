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
  <table>
    <#if product?exists>
        <@displayRow title=uiLabelMap.ProductProductName text="${product.internalName} (${product.productId})"/>
        <@displayRow title=uiLabelMap.CommonStatus text=statusItem.description/>
        
        <#assign toProduceMap = opentapsProductionRun.getProductsToProduce()/>
        <#-- because pruns from MRP might have wegs with fromDate in the future until they are actually started, quantityToProduce might be empty-->
        <#assign quantityToProduce = toProduceMap.get(product.productId)! />
    </#if>

    <#if opentapsProductionRun.isAssembly()>
        <@displayRow title=uiLabelMap.ManufacturingQuantityToProduce text=quantityToProduce/>
    <#else>
        <@displayRow title=uiLabelMap.WarehouseQuantityToDisassemble text=quantityToProduce/>
    </#if>
    <@displayDateRow title=uiLabelMap.ManufacturingEstimatedStartDate date=productionRunData.estimatedStartDate/>
    <@displayDateRow title=uiLabelMap.ManufacturingActualStartDate date=productionRunData.actualStartDate/>
    <@displayDateRow title=uiLabelMap.ManufacturingEstimatedCompletionDate date=productionRunData.estimatedCompletionDate/>
    <@displayDateRow title=uiLabelMap.ManufacturingActualCompletionDate date=productionRunData.actualCompletionDate/>
    <@displayRow title=uiLabelMap.ManufacturingProductionRunName text=productionRunData.productionRunName/>
    <@displayRow title=uiLabelMap.CommonDescription text=productionRunData.description/>
  </table>
