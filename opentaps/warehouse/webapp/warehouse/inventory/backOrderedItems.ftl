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

<div class="subSectionBlock">
<form method="get" target="" name="findBackOrderedItems">
<table class="twoColumnForm">
  <tr>
    <td class="titleCell"><span class="tableheadtext">${uiLabelMap.PageTitleOrderStatus}</span></td>
    <td>
      <select name="statusId" class="inputBox">
        <option value="ORDER_APPROVED">${uiLabelMap.CommonApproved}</option>
        <option value="ORDER_CREATED" <#if parameters.statusId?default("") == "ORDER_CREATED">selected</#if>>${uiLabelMap.CommonCreated}</option>
        <option value="ORDER_HOLD" <#if parameters.statusId?default("") == "ORDER_HOLD">selected</#if>>${uiLabelMap.CrmActivityOnHold}</option>
        <option value="ANY" <#if parameters.statusId?default("") == "ANY">selected</#if>>${uiLabelMap.CommonAny}</option>
      </select>
    </td>
  </tr>
  <@inputAutoCompleteProductRow name="productId" title=uiLabelMap.ProductProductId default=parameters.productId />
  <tr>
    <td class="titleCell"><span class="tableheadtext">${uiLabelMap.ProductShipmentMethod}</span></td>
    <td>
      <select name="carrierAndShipmentMethodTypeId" class="inputBox">
        <option value=""></option>
        <#list shipmentMethodTypes?sort_by("carrierPartyName") as option>
          <#if (option.partyId + "^" + option.shipmentMethodTypeId) == parameters.carrierAndShipmentMethodTypeId?if_exists>
            <#assign selected = "selected"><#else><#assign selected = "">
          </#if>
          <option ${selected} value="${option.partyId}^${option.shipmentMethodTypeId}">
            ${option.carrierPartyName} ${option.description}
          </option>
        </#list>
      </select>
    </td>
  </tr>
  <@inputSubmitRow title=uiLabelMap.CommonFind />
</table>
</form>
</div>

<table class="listTable">
    <tr style="vertical-align:bottom">
        <td><span class="tableheadtext">${uiLabelMap.OpentapsReservationSequence}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductProductId}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.OrderOrderDate}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.OrderOrder}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.CommonStatus}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.OpentapsShipVia}</span></td>
        <td class="fieldWidth50"><span class="tableheadtext">${uiLabelMap.OpentapsQtyOrdered}</span></td>
        <td class="fieldWidth50"><span class="tableheadtext">${uiLabelMap.OpentapsQtyBackOrdered}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.OpentapsPromisedDate}</span></td>        
    </tr>
    <#list backOrderedItems?if_exists as item>
        <#assign itemInfo = item.orderId />
        <#if item.getRelatedOneCache("ShipmentMethodType")?exists>
          <#assign shipmentMethodTypeDescription = " " + item.getRelatedOneCache("ShipmentMethodType").description />
        <#else>
          <#assign shipmentMethodTypeDescription = " " />
        </#if>
        <#assign shipmentInfo = item.carrierPartyId?default("N/A") +  shipmentMethodTypeDescription/>
        <#if item.promisedDatetime?exists><#assign promisedDate = item.promisedDatetime?string.short></#if>
    <tr class="${tableRowClass(item_index)}">
        <@displayDateCell date=item.reservedDatetime />
        <@displayCell text=item.productId />
        <@displayDateCell date=item.orderDate />
        <@displayLinkCell href="shipGroups.pdf?orderId=${item.orderId}&shipGroupSeqId=${item.shipGroupSeqId}" text=itemInfo />
        <@displayCell text=item.getRelatedOneCache("OrderStatusItem").description />
        <@displayCell text=shipmentInfo />
        <@displayCell text=item.quantityReserved />
        <@displayCell text=item.quantityNotAvailable />
        <@displayDateCell date=promisedDate />
    </tr>
    </#list>
</table>

<#assign exParams>&productId=${productId?if_exists}&carrierAndShipmentMethodTypeId=${parameters.carrierAndShipmentMethodTypeId?if_exists}</#assign>
<@pagination viewIndex=viewIndex viewSize=viewSize currentResultSize=backOrderedItems?size requestName="backOrderedItems" totalResultSize=backOrderedItemsTotalSize extraParameters=exParams/>
