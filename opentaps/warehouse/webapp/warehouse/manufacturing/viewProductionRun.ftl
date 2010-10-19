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
