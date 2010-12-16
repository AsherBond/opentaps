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

<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#if shipmentItemDatas?has_content>
  <br/>
  <table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductItem}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">${uiLabelMap.WarehouseLot}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductSerialNumber}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductQuantity}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list shipmentItemDatas as shipmentItemData>
        <#assign shipmentItem = shipmentItemData.shipmentItem>
        <#assign itemIssuances = shipmentItemData.itemIssuances>
        <#assign orderShipments = shipmentItemData.orderShipments>
        <#assign shipmentPackageContents = shipmentItemData.shipmentPackageContents>
        <#assign product = shipmentItemData.product?if_exists>
        <tr>
            <td><div class="tabletext">${shipmentItem.shipmentItemSeqId}</div></td>
            <td colspan="4"><div class="tabletext">${(product.internalName)?if_exists} [<@displayLink href="/catalog/control/EditProduct?productId=${shipmentItem.productId?if_exists}" text="${shipmentItem.productId?if_exists}" target="_blank"/>]</div></td>
            <td><div class="tabletext">${shipmentItem.quantity?default("&nbsp;")}</div></td>
            <td colspan="2"><div class="tabletext">${shipmentItem.shipmentContentDescription?default("&nbsp;")}</div></td>
        </tr>
        <#list orderShipments as orderShipment>
            <tr>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">${uiLabelMap.ProductOrderItem} :<@displayLink href="/crmsfa/control/orderview?orderId=${orderShipment.orderId?if_exists}" text="${orderShipment.orderId?if_exists}" target="_blank" />:${orderShipment.orderItemSeqId?if_exists}</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">${orderShipment.quantity?if_exists}</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
            </tr>
        </#list>
        <#list itemIssuances as itemIssuance>
            <tr>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">${uiLabelMap.ProductOrderItem} :<@displayLink href="/crmsfa/control/orderview?orderId=${itemIssuance.orderId?if_exists}" text="${itemIssuance.orderId?if_exists}" target="_blank" />:${itemIssuance.orderItemSeqId?if_exists}</div></td>
                <td><div class="tabletext">${uiLabelMap.ProductInventory} :<@displayLink href="EditInventoryItem?inventoryItemId=${itemIssuance.inventoryItemId?if_exists}" text="${itemIssuance.inventoryItemId?if_exists}" target="_blank" /></div></td>
                <td><div class="tabletext"><#if itemIssuance.lotId?exists><@displaylink href="lotDetails?lotId=${itemIssuance.lotId?if_exists}" text="${itemIssuance.lotId?if_exists}" /><#else>&nbsp;</#if></div></td>
                <td><div class="tabletext"><#if itemIssuance.serialNumber?exists>${itemIssuance.serialNumber?if_exists}<#else>&nbsp;</#if></div></td>
                <td><div class="tabletext">${itemIssuance.quantity?if_exists}</div></td>
                <td><div class="tabletext">${getLocalizedDate(itemIssuance.issuedDateTime)}</div></td>
                <td><div class="tabletext">${uiLabelMap.ProductFuturePartyRoleList}</div></td>
            </tr>
        </#list>
        <#list shipmentPackageContents as shipmentPackageContent>
            <tr>
                <td><div class="tabletext">&nbsp;</div></td>
                <td colspan="4"><div class="tabletext">${uiLabelMap.ProductPackage} :${shipmentPackageContent.shipmentPackageSeqId}</div></td>
                <td><div class="tabletext">${shipmentPackageContent.quantity?if_exists}</div></td>
                <td colspan="2"><div class="tabletext">&nbsp;</div></td>
            </tr>
        </#list>
    </#list>
  </table>
</#if>
