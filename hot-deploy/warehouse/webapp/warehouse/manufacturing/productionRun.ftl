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

<#if productionRun?has_content>
    <#if productProducedMissing?has_content>
      <div class="errorMessageHeader" style="margin-bottom:10px;padding:10px;border:1px dashed red">No product produced was defined for this production run, a WorkEffortGoodStandard of type PRUN_PROD_DELIV is required and it may have been removed by mistake.<br/>
To solve this problem, go to <@displayLink href="/workeffort/control/EditWorkEffortGoodStandards?workEffortId=${productionRun.workEffortId}&amp;estimatedQuantity=${productionRun.quantityToProduce?if_exists}&amp;fromDate=${productionRun.estimatedStartDate?if_exists}&amp;productId=${(productionRun.workEffortName?split(\"-\")[0])?if_exists}&amp;statusId=WEGS_CREATED&amp;workEffortGoodStdTypeId=PRUN_PROD_DELIV" text="this page"/> to add the needed Good Standard of type "Production Run and Product to Deliver Association" (please check that the guessed values for product ID and estimated quantity are correct), and come back here.</div>
    </#if>
    <#if productionRun.currentStatusId == "PRUN_CREATED" || productionRun.currentStatusId == "PRUN_SCHEDULED">
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#EditProductionRun")}
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunTasks")}
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunMaterials")}
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunFixedAssets")}
        <div class="spacer">&nbsp;</div>
    <#else>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ViewProductionRun")}
        <div class="spacer">&nbsp;</div>
        <#if productionRun.currentStatusId == "PRUN_COMPLETED">
            ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunSerialNumbers")}
            <div class="spacer">&nbsp;</div>
        </#if>
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunRoutingTasks")}
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ProductionRunActualMaterials")}
        <div class="spacer">&nbsp;</div>
        ${screens.render("component://warehouse/widget/warehouse/screens/manufacturing/ManufacturingScreens.xml#ActiveProductionRunFixedAssets")}
    </#if>
</#if>
