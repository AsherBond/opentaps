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
<#assign rowIndex = 0 />
<form action="<@ofbizUrl>updateInventoryItemsProdRun</@ofbizUrl>" method="post">
    <@inputHidden name="productionRunId" value="${productionRunId}"/>
    <table class="listTable" cellspacing="0" style="width:50%">
        <tr class="listTableHeader">
            <td>${uiLabelMap.ProductInventoryItemId}</td>
            <td>${uiLabelMap.ProductProductId}</td>
            <td>${uiLabelMap.ProductSerialNumber}</td>
        </tr>
        <#list inventoryProduced as inventoryItemProduced>
            <#assign inventoryItem = inventoryItemProduced.getRelatedOne("InventoryItem")/>
            <#if inventoryItem.inventoryItemTypeId == "SERIALIZED_INV_ITEM">
                <tr class="${tableRowClass(rowIndex)}">
                    <@displayLinkCell href="EditInventoryItem?inventoryItemId=${inventoryItem.inventoryItemId}" text="${inventoryItem.inventoryItemId}"/>
                    <@displayCell text="${inventoryItem.productId}"/>
                    <@inputTextCell name="serialNumber_o_${rowIndex}" default="${inventoryItem.serialNumber?if_exists}"/>
                    <@inputHidden name="_rowSubmit_o_${rowIndex}" value="Y"/>
                    <@inputHidden name="inventoryItemId_o_${rowIndex}" value="${inventoryItem.inventoryItemId}"/>
                </tr>
                <#assign rowIndex = rowIndex + 1 />
            </#if>
        </#list>
        <@inputHidden name="_rowCount" value="${rowIndex}"/>
        <@inputSubmitRow title="${uiLabelMap.CommonUpdate}"/>
    </table>
</form>
