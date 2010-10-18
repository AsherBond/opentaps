<#--
 * Copyright (c) Open Source Strategies, Inc.
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

<table class="listTable">
    <tr style="vertical-align:bottom">
        <th class="leftAlign">${uiLabelMap.OrderOrderDate}</th>
        <th class="leftAlign">${uiLabelMap.OrderOrderName}</th>
        <th class="leftAlign">${uiLabelMap.ProductSupplier}</th>
        <th class="leftAlign">${uiLabelMap.CommonStatus}</th>
        <th class="rightAlign">${uiLabelMap.CommonAmount}</th>
    </tr>
    <#list purchaseOrders?if_exists as order>
    <tr class="${tableRowClass(order_index)}">
        <@displayDateCell date=order.orderDate />
        <#if sectionName?if_exists == "lookup">
            <@displayLinkCell href="javascript:set_value('${order.orderId?if_exists}')" text="${order.orderName?if_exists} (${order.orderId?if_exists})" />
        <#else>
            <@displayLinkCell href="orderview?orderId=${order.orderId?if_exists}" text="${order.orderName?if_exists} (${order.orderId?if_exists})" />
        </#if>
        <@displayCell text="${order.partyName?if_exists} (${order.partyId?if_exists})" />
        <@displayCell text=order.statusDescription?if_exists />
        <@displayCurrencyCell currencyUomId=order.currencyUom?if_exists amount=order.grandTotal?default(0.0) />
    </tr>
    </#list>
</table>
<#if purchaseOrders?exists>
    <#-- The reason why "thisRequestUri" is used is because there are at least three -->
    <#-- URI's that make use of this FTL file, and thus it will not be simple to     -->
    <#-- decide what URI should be given to the <@pagination> macro.  Using          -->
    <#-- "thisRequestUri" will provide flexibility with this regard, and the         -->
    <#-- pagination URI's will always have the same target as the currently viewed   -->
    <#-- page.                                                                       -->

    <@pagination viewIndex=viewIndex viewSize=viewSize currentResultSize=purchaseOrders?size requestName=parameters.thisRequestUri totalResultSize=purchaseOrdersTotalSize extraParameters=extraParameters/>
</#if>

