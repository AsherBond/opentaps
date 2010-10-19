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

<script type="text/javascript">
  refreshMillis = 120000;
  setTimeout('window.location.replace(window.location.href)', refreshMillis);
</script>

<table class="listTable" cellspacing="0" cellpadding="5">
    <tr class="listTableHeader">
        <td>${uiLabelMap.OrderOrderDate}</td>
        <td>${uiLabelMap.WarehouseOrderShipGroup}</td>
        <td>${uiLabelMap.CommonFor}</td>
        <td>${uiLabelMap.OpentapsShipVia}</td>
        <td>${uiLabelMap.WarehouseOrderShipByDate}</td>
        <td>&nbsp;</td>
    </tr>
    <#list orders as order>
        <#assign rowClass = (order_index % 2 == 0)?string("rowWhite", "rowLightGray")/>
        <tr class="${rowClass}">
            <td>
                ${getLocalizedDate(order.orderDate, "DATE_TIME")}
            </td>
            <td>
                <a href="<@ofbizUrl>shipGroups.pdf?orderId=${order.orderId}&amp;shipGroupSeqId=${order.shipGroupSeqId}</@ofbizUrl>" class="linktext">${order.orderId} / ${order.shipGroupSeqId}</a> (PDF)
            </td>
            <td>
                ${Static["org.opentaps.common.order.UtilOrder"].getBillToCustomerPartyName(delegator, order.orderId, false)?if_exists}
            </td>
            <td>
                <#assign carrier = order.getRelatedOne("Party")!/>
                <#if carrier?has_content>
                    ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(carrier)}
                </#if>
                <#assign shipmentMethodType = order.getRelatedOne("ShipmentMethodType")!/>
                <#if shipmentMethodType?has_content>
                    &nbsp;${shipmentMethodType.description}
                </#if>
            </td>
            <td>
                <#if order.shipByDate?has_content>
                    ${getLocalizedDate(order.shipByDate, "DATE")}
                </#if>
            </td>
            <td class="alignRight">
                <a href="<@ofbizUrl>PackOrder?orderId=${order.orderId}&amp;shipGroupSeqId=${order.shipGroupSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.WarehousePackOrder}</a>
            </td>
        </tr>
    </#list>
</table>

<@pagination viewIndex=viewIndex viewSize=viewSize currentResultSize=orders?size requestName="readyToShip" totalResultSize=ordersTotalSize/>

