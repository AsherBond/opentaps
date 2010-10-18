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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign directions = {"FORWARD":uiLabelMap.WarehouseTraceForward, "BACKWARD":uiLabelMap.WarehouseTraceBackward} >

<div class="subSectionBlock">
    <div class="form">
        <form method="post" action="<@ofbizUrl>traceInventory</@ofbizUrl>" name="TraceInventoryForm">
            <table>
                <@inputTextRow name="lotId" title=uiLabelMap.ProductLotId size="20" maxlength="20"/>
                <@inputTextRow name="inventoryItemId" title=uiLabelMap.ProductInventoryItemId size="20" maxlength="20"/>
                <@inputSelectHashRow name="traceDirection" title=uiLabelMap.OpentapsDirection hash=directions required=true/>
                <@inputHidden name="performFind" value="Y" />
                <@inputSubmitRow title=uiLabelMap.CommonFind />
            </table>
        </form>
    </div>
</div>

<#if direction?has_content>
    <#if direction.equals("BACKWARD")>
        <#assign subSectionTitle = uiLabelMap.WarehouseTraceSourceInventoryItems/>
    <#elseif direction.equals("FORWARD")>
        <#assign subSectionTitle = uiLabelMap.WarehouseTraceDerivativeInventoryItems/>
    </#if>
</#if>

<#if traceLog?has_content> 
<div class="subSectionBlock">
<@sectionHeader title=subSectionTitle/>

    <table class="crmsfaListTable" cellspacing="0">
        <tr class="crmsfaListTableHeader">
            <@displayCell text=uiLabelMap.WarehouseTraceLevel class="tableheadtext"/>
            <@displayCell text=uiLabelMap.ProductInventoryItemId class="tableheadtext"/>
            <@displayCell text=uiLabelMap.WarehouseToInventoryItemId class="tableheadtext"/>
            <@displayCell text=uiLabelMap.CommonDate class="tableheadtext"/>
            <@displayCell text=uiLabelMap.WarehouseTraceUsageType class="tableheadtext"/>
            <@displayCell text=uiLabelMap.CommonQuantity class="tableheadtext"/>
            <@displayCell text=uiLabelMap.OrderOrderId class="tableheadtext"/>
            <@displayCell text=uiLabelMap.WarehouseProductionRunId class="tableheadtext"/>
        </tr>

        <#list traceLog as usageList>
 
            <#list usageList as usage>
            <#assign usageType = usage.usageType/>

            <tr class="${tableRowClass(usage_index + 1)}"/>
                <@displayCell text=usage.traceLevel/>
                <td>
                    <span class="tabletext">
                        <@displayLink text=usage.inventoryItemId?default("") href="EditInventoryItem?inventoryItemId=${usage.inventoryItemId?default('')}"/>
                        <#if usage.fromProductId?has_content>
                        (${uiLabelMap.ProductProduct} <@displayLink text="${usage.fromProductId?if_exists}" href="findInventoryItem?productId=${usage.fromProductId?if_exists}&performFind=Y" />
                        <#if usage.fromLotId?has_content> ${uiLabelMap.WarehouseLot} <@displayLink text="${usage.fromLotId}" href="lotDetails?lotId=${usage.fromLotId}"/></#if>)
                        </#if>
                    </span>
                </td>
                <td>
                    <span class="tabletext">
                        <@displayLink text=usage.toInventoryItemId?default("") href="EditInventoryItem?inventoryItemId=${usage.toInventoryItemId?default('')}"/>
                        <#if usage.toProductId?has_content>
                        (${uiLabelMap.ProductProduct} <@displayLink text="${usage.toProductId?if_exists}" href="findInventoryItem?productId=${usage.toProductId?if_exists}&performFind=Y" />
                        <#if usage.toLotId?has_content> ${uiLabelMap.WarehouseLot} <@displayLink text="${usage.toLotId}" href="lotDetails?lotId=${usage.toLotId}"/></#if>)
                        </#if>
                    </span>
                </td>
                <@displayCell text="${getLocalizedDate(usage.usageDatetime?if_exists)}"/>
                <#if "TRANSFER" == usage.inventoryItemUsageTypeId>
                <td>
                    <@display text="${usageType.description} "/>
                    <@displayLink text=usage.inventoryTransferId?if_exists href="TransferInventoryItem?inventoryTransferId=${usage.inventoryTransferId?if_exists}" target="_blank"/>
                </td>
                <#elseif "VARIANCE" == usage.inventoryItemUsageTypeId>
                 <#assign varianceReason = usage.varianceReason?if_exists/>
                    <@displayCell text="${usageType.description}, ${uiLabelMap.WarehouseReasonIs} ${varianceReason.getDescription()?if_exists}"/>
                <#else>
                    <@displayCell text=usageType.description/>
                </#if>
                <@displayCell text=usage.quantity?if_exists/>
                <td>
                    <@displayLink text=usage.orderId?if_exists href="/crmsfa/control/orderview?orderId=${usage.orderId?if_exists}" target="_blank"/> <#if usage.correspondingPoId?has_content><@display text=" (${uiLabelMap.OpentapsPONumber}${usage.correspondingPoId})" /></#if>
                </td>
                <@displayLinkCell text=usage.productionRunId?if_exists href="ShowProductionRun?productionRunId=${usage.productionRunId?if_exists}" target="_blank"/>
            </tr>
            </#list>

            <#-- put empty line between different inventory items -->
            <#if usageList_has_next>
                <tr><td colspan="7">&nbsp;</td></tr>
            </#if>

        </#list>
    </table>

</div>
</#if>
