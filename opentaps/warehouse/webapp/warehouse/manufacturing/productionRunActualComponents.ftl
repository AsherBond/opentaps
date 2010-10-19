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

<#if hasIssuableMaterials>
<script type="text/javascript">
    function rowClassChange(/*Element*/ input, /*Number*/ rowIndex) {
        // trim the input and replace empty by "0"
        if (input.value != null && input.value != "0") {
            input.value = input.value.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
            input.value = input.value.replace(/^00*/, '');
            if (input.value == "") input.value = "0";
        }
        // color the row according to the value: 0 and positive_integer=>normal invalid=>red
        if (input.value != null && input.value != "0" && (parseInt(input.value) != input.value - 0 || parseInt(input.value) < 0)) {
            input.parentNode.parentNode.className = 'rowLightRed';
        } else {
            input.parentNode.parentNode.className = rowIndex % 2 == 0 ? 'rowWhite' : 'rowLightGray';
        }
    }
</script>
</#if>

<#if hasRunningTasks>
    <#assign issueLink><a href="<@ofbizUrl>issueAdditionalMaterialForm?productionRunId=${productionRun.workEffortId}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.WarehouseIssueAdditionalMaterial}</a></#assign>
</#if>

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.WarehouseManufacturingMaterials}</div>
    <div class="subMenuBar">${issueLink?if_exists}</div>
</div>
        
<form action="<@ofbizUrl>issueMaterialsCheck</@ofbizUrl>" method="post" name="issueMaterials">
    <@inputHidden name="productionRunId" value=parameters.productionRunId />
    <@inputHiddenUseRowSubmit />
    <@inputHiddenRowCount list=productionRunComponentsData />

<table class="listTable" cellspacing="0" style="width:100%">
    <tr class="listTableHeader">
        <td>${uiLabelMap.ManufacturingRoutingTaskId}</td>
        <td>${uiLabelMap.ProductProductName}</td>
        <td>${uiLabelMap.WarehouseQuantityQOH}</td>
        <td>${uiLabelMap.WarehouseQuantityQtyIssued}</td>
        <#if hasIssuableMaterials>
          <td>${uiLabelMap.OpentapsNeeded}</td>
          <td>${uiLabelMap.ProductIssue}</td>
        </#if>
    </tr>
    <#list productionRunComponentsData?sort_by("productId") as component>
        <@inputHidden name="workEffortId" value=component.workEffortId index=component_index />
        <@inputHidden name="productId" value=component.productId index=component_index />
        <tr class="${tableRowClass(component_index)}">
            <@displayCell text="${component.workEffortName} [${component.workEffortId}]"/>
            <@displayCell text="${component.internalName} [${component.productId}]"/>
            <@displayCell text=component.qoh?default(0)/>
            <td>
                ${component.issuedQuantity}
                <#if component.quantitiesByLot.size() != 0>
                  (
                  <#list component.quantitiesByLot.keySet() as lotId>
                    <#if lotId?exists>
                      <a href="<@ofbizUrl>lotDetails?lotId=${lotId}</@ofbizUrl>" class="linktext">${uiLabelMap.WarehouseLot} ${lotId}:</a>
                      ${component.quantitiesByLot.get(lotId)}
                    </#if>
                  </#list>
                  )
                </#if>
            </td>
            <#if hasIssuableMaterials>
              <#if component.remainingQuantity?exists>
                <@inputHiddenRowSubmit index=component_index />
                <td onClick="document.issueMaterials.quantity_o_${component_index}.value = '${component.remainingQuantity}'">${component.remainingQuantity}</td>
                <td>
                    <#assign default = component.remainingQuantity>
                    <#if parameters.forceComplete?default("false") == "true">
                       <#assign default = parameters.get("quantity_o_${component_index}")>
                    </#if>
                    <input type="text" class="inputBox" size="6" name="quantity_o_${component_index}" id="quantity_o_${component_index}" onchange="rowClassChange(this, ${component_index})" value="${default}" />
                    <@displayError name="quantity" index=component_index />
                </td>
              <#else>
                <@inputHiddenRowSubmit index=component_index submit=false />
                <td/><td/>
              </#if>
            </#if>
        </tr>
    </#list>
    <#if hasIssuableMaterials>
        <tr>
            <td colspan="5"></td>
            <@inputForceCompleteCell title=uiLabelMap.ProductIssue forceTitle=uiLabelMap.OpentapsForceIssue form="issueMaterials" />
        </tr>
    </#if>
</table>
