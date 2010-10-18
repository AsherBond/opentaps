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

<#-- This file has been modified from the version included with the Apache-licensed OFBiz facility application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<@frameSection title="${uiLabelMap.ProductInventoryTransfersFor} ${facility?exists?string((facility.facilityName)!,'')} [${uiLabelMap.CommonId}:${facilityId?if_exists}]">

<#if activeOnly>
  <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=false</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveAndInactive}</a>
<#else>
  <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveOnly}</a>
</#if>
<a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&completeRequested=true</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCompleteRequestedTransfers}</a>
<a href="<@ofbizUrl>TransferInventoryItem?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductInventoryTransfer}</a>

<br/>
<#if toTransfers?has_content>
  <br/>
  <@frameSection title="${uiLabelMap.CommonTo}:&nbsp;${facility?exists?string((facility.facilityName)!,'')} [${uiLabelMap.CommonId}:${facilityId?if_exists}]" innerStyle="padding:0;border:0">
    <table class="listTable">
      <tr class="listTableHeader">
        <td>${uiLabelMap.ProductTransferId}</td>
        <td>${uiLabelMap.ProductItem}</td>
        <td>${uiLabelMap.ProductProductId}</td>
        <td>${uiLabelMap.ProductInternalName}</td>
        <td>${uiLabelMap.ProductSerialAtpQoh}</td>
        <td>${uiLabelMap.CommonFrom}</td>
        <td>${uiLabelMap.CommonSendDate}</td>
        <td>${uiLabelMap.CommonStatus}</td>
        <td>&nbsp;</td>
      </tr>

      <#list toTransfers as transfer>
        <#assign inventoryItem = transfer.getRelatedOneCache("InventoryItem")?if_exists/>
        <#if inventoryItem?has_content>
          <#assign product = inventoryItem.getRelatedOneCache("Product")?if_exists/>
        </#if>
        <#assign fac = transfer.getRelatedOneCache("Facility")?if_exists/>
        <tr class="${tableRowClass(transfer_index)}">
          <@displayLinkCell href="TransferInventoryItem?inventoryTransferId=${transfer.inventoryTransferId!}" text=transfer.inventoryTransferId! />
          <@displayLinkCell href="EditInventoryItem?inventoryItemId=${transfer.inventoryItemId!}" text=transfer.inventoryItemId! />
          <@displayCell text=(product.productId)! />
          <@displayCell text=(product.internalName)! />
          <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
            <@displayCell text="${(inventoryItem.availableToPromiseTotal)!}&nbsp;/&nbsp;${(inventoryItem.quantityOnHandTotal)!}" />
          <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
            <@displayCell text=inventoryItem.serialNumber! />
          </#if>
          <@displayCell text="${(fac.facilityName)!}&nbsp;[${(transfer.facilityId)!}]" />
          <@displayDateCell date=transfer.sendDate! />
          <td>
            <#if (transfer.statusId)?exists>
              <#assign transferStatus = transfer.getRelatedOneCache("StatusItem")?if_exists />
              <@display text="${(transferStatus.get(\"description\", locale))!}" />
            </#if>
          </td>
          <td align="center">
            <@displayLink href="TransferInventoryItem?inventoryTransferId=${transfer.inventoryTransferId!}" text=uiLabelMap.CommonEdit class="buttontext"/>
          </td>
        </tr>
      </#list>
    </table>
  </@frameSection>
</#if>

<#if fromTransfers?has_content>
  <#if completeRequested>
    <form name="CompleteRequested" method="post" action="CheckInventoryForCompleteRequestedTransfers?completeRequested=true&facilityId=${facility.facilityId}">
  </#if>
  <@frameSection title="${uiLabelMap.CommonFrom}:&nbsp;${facility?exists?string((facility.facilityName)!,'')} [${uiLabelMap.CommonId}:${facilityId?if_exists}]" innerStyle="padding:0;border:0">
    <table class="listTable">
      <tr class="listTableHeader">
        <td>${uiLabelMap.ProductTransferId}</td>
        <td>${uiLabelMap.ProductItem}</td>
        <td>${uiLabelMap.ProductProductId}</td>
        <td>${uiLabelMap.ProductInternalName}</td>
        <td>${uiLabelMap.ProductSerialAtpQoh}</td>
        <td>${uiLabelMap.CommonTo}</td>
        <td>${uiLabelMap.CommonSendDate}</td>
        <#if !completeRequested>
          <td>${uiLabelMap.CommonStatus}</td>
        </#if>
        <td align="center">
          <#if completeRequested><@inputMultiSelectAll form="CompleteRequested" /><#else>&nbsp;</#if>
        </td>
      </tr>

      <#list fromTransfers as transfer>
        <#assign inventoryItem = transfer.getRelatedOneCache("InventoryItem")?if_exists/>
        <#if inventoryItem?has_content>
          <#assign product = inventoryItem.getRelatedOneCache("Product")?if_exists/>
        </#if>
        <#assign fac = transfer.getRelatedOneCache("ToFacility")?if_exists/>
        <tr class="${tableRowClass(transfer_index)}">
          <@displayLinkCell href="TransferInventoryItem?inventoryTransferId=${transfer.inventoryTransferId!}" text=transfer.inventoryTransferId! />
          <@displayLinkCell href="EditInventoryItem?inventoryItemId=${transfer.inventoryItemId!}" text=transfer.inventoryItemId! />
          <@displayCell text=(product.productId)! />
          <@displayCell text=(product.internalName)! />
          <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
            <@displayCell text="${(inventoryItem.availableToPromiseTotal)!}&nbsp;/&nbsp;${(inventoryItem.quantityOnHandTotal)!}" />
          <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
            <@displayCell text=inventoryItem.serialNumber! />
          </#if>
          <@displayCell text="${(fac.facilityName)!}&nbsp;[${(transfer.facilityIdTo)!}]" />
          <@displayDateCell date=transfer.sendDate! />
          <#if !completeRequested>
            <td>
              <#if (transfer.statusId)?exists>
                <#assign transferStatus = transfer.getRelatedOneCache("StatusItem")?if_exists />
                <@display text="${(transferStatus.get(\"description\", locale))!}" />
              </#if>
            </td>
          </#if>
          <td align="center">
            <#if completeRequested>
              <@inputHidden index=transfer_index name="inventoryTransferId" value=transfer.inventoryTransferId!/>
              <@inputHidden index=transfer_index name="inventoryItemId" value=transfer.inventoryItemId!/>
              <@inputHidden index=transfer_index name="statusId" value="IXF_COMPLETE"/>
              <@inputMultiCheck index=transfer_index/>
            <#else>
              <@displayLink href="TransferInventoryItem?inventoryTransferId=${transfer.inventoryTransferId!}" text=uiLabelMap.CommonEdit class="buttontext"/>
            </#if>
          </td>
        </tr>
        <#assign rowCount = transfer_index + 1/>
      </#list>

      <#if completeRequested>
        <tr>
          <td colspan="8" align="right">
            <@inputHidden name="_rowCount" value="${rowCount}"/>
            <@inputHiddenUseRowSubmit />
            <@inputHidden name="forceComplete" value="${parameters.forceComplete?default(\"false\")}"/>
            <#if parameters.forceComplete?default("false") == "true">
              <@inputConfirm title=uiLabelMap.WarehouseForceComplete form="CompleteRequested" />
            <#else>
              <@inputSubmit title=uiLabelMap.ProductComplete />
            </#if>
          </td>
        </tr>
      </#if>
    </table>
  </@frameSection>
  <#if completeRequested>
    </form>
  </#if>
</#if>

</@frameSection>
