<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
        <@displayCell text="${order.supplierPartyName?if_exists} (${order.supplierPartyId?if_exists})" />
        <@displayCell text=order.statusDescription?if_exists />
        <@displayCurrencyCell currencyUomId=order.currencyUom?if_exists amount=order.grandTotal?if_exists />
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
