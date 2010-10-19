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

<#if inventoryItemId?exists>
  <#assign actionLinks><a href="<@ofbizUrl>traceInventory?inventoryItemId=${inventoryItemId}&performFind=Y&traceDirection=FORWARD</@ofbizUrl>" class="subMenuButton">${uiLabelMap.WarehouseTraceInventory}</a><a href="<@ofbizUrl>TransferInventoryItem?inventoryItemId=${inventoryItemId}<#if facilityId?exists>&facilityId=${facilityId}</#if></@ofbizUrl>" class="subMenuButton">${uiLabelMap.ProductTransferItem}</a><a href="<@ofbizUrl>ViewInventoryItemDetail?inventoryItemId=${inventoryItemId}<#if facilityId?exists>&facilityId=${facilityId}</#if></@ofbizUrl>" class="subMenuButton">${uiLabelMap.ProductInventoryDetails}</a></#assign>
</#if>

<@frameSection title="${uiLabelMap.ProductEditInventoryItemWithId} [${inventoryItemId?if_exists}]" extra=actionLinks?if_exists>

<#if inventoryItem?exists>
  <form action="<@ofbizUrl>UpdateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
    <table style="border:0" class="twoColumnForm">
      <@inputHidden name="inventoryItemId" value=inventoryItemId />
      <@displayRow title=uiLabelMap.ProductInventoryItemId text="<b>${inventoryItemId}</b> ${uiLabelMap.ProductNotModificationRecrationInventoryItem}" />
<#else>
  <#if inventoryItemId?exists>
    <form action="<@ofbizUrl>CreateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
      <table style="border:0" class="twoColumnForm">
        <h3>${uiLabelMap.ProductNotFindInventoryItemWithId} "${inventoryItemId}".</h3>
  <#else>
    <form action="<@ofbizUrl>CreateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
      <table style="border:0" class="twoColumnForm">
  </#if>
</#if>
      <@inputHidden name="validateAccountingTags" value="True"/>
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductInventoryItemTypeId />
        <td>
          <select name="inventoryItemTypeId" size="1" class="selectBox">
            <#if inventoryItemType?exists>
              <option selected="selected" value="${inventoryItemType.inventoryItemTypeId}">${inventoryItemType.get("description",locale)}</option>
              <option value="${inventoryItemType.inventoryItemTypeId}">----</option>
            </#if>
            <#list inventoryItemTypes as nextInventoryItemType>
              <option value="${nextInventoryItemType.inventoryItemTypeId}">${nextInventoryItemType.get("description",locale)}</option>
            </#list>
          </select>
        </td>
      </tr>
      <#if (inventoryItem.productId)?has_content>
        <#assign product = inventoryItem.getRelatedOne("Product")/>
      </#if>
      <@displayRow title=uiLabelMap.ProductProductId text="${(product.internalName)} (${(product.productId)!})" />
      <@inputLookupRow title=uiLabelMap.PartyPartyId name="partyId" lookup="LookupPartyName" form="inventoryItemForm" default=(inventoryItemData.partyId)! />
      <@inputLookupRow title=uiLabelMap.ProductFacilityOwner name="ownerPartyId" lookup="LookupPartyName" form="inventoryItemForm" default=(inventoryItemData.ownerPartyId)! />
      <#if "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
          <tr>
            <@displayTitleCell title=uiLabelMap.ProductStatus />
            <td>
              <select name="statusId" class="selectBox">
                  <#if (inventoryItem.statusId)?has_content>
                      <option value="${inventoryItem.statusId}">${(curStatusItem.get("description",locale))?default("[" + inventoryItem.statusId + "]")}</option>
                      <option value="${inventoryItem.statusId}">----</option>
                  </#if>
                  <#if !tryEntity && requestParameters.statusId?has_content>
                      <#assign selectedStatusId = requestParameters.statusId>
                  </#if>
                  <#list statusItems as statusItem>
                      <option value="${statusItem.statusId}"<#if selectedStatusId?if_exists == statusItem.statusId>${uiLabelMap.ProductSelected}</#if>>${statusItem.get("description",locale)}</option>
                  </#list>
              </select>
            </td>
          </tr>
      </#if>
      <@inputDateTimeRow title=uiLabelMap.ProductDateReceived name="datetimeReceived" default=(inventoryItemData.datetimeReceived)?if_exists />
      <@inputDateTimeRow title=uiLabelMap.ProductExpireDate name="expireDate" default=(inventoryItemData.expireDate)?if_exists />
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductFacility />
        <td>
            <span class="tabletext">
            <#if inventoryItem?exists>
                   ${(facility.facilityName)?if_exists} [${inventoryItem.facilityId}]
                   <@inputHidden name="facilityId" value=facilityId! />
            <#else>
                   ${uiLabelMap.ProductSelectFacility} : 
            </#if>
            </span>
            <#if !inventoryItem?exists>
            <select name="facilityId" class="selectBox">
              <#if !tryEntity && requestParameters.facilityId?has_content>
                  <#assign selectedFacilityId = requestParameters.facilityId>
              </#if>
              <#list facilities as nextFacility>
                <option value="${nextFacility.facilityId}"<#if selectedFacilityId?if_exists == nextFacility.facilityId> ${uiLabelMap.ProductSelected}</#if>>${nextFacility.facilityName?if_exists} [${nextFacility.facilityId}]</option>
              </#list>
            </select>
            <br/>
            <span class="tabletext">${uiLabelMap.ProductOrEnterContainerId} :</span>
            <@inputText name="containerId" default=(inventoryItemData.containerId)! />
            </#if>
         </td>
       </tr>
      <tr>
        <@displayTitleCell title=uiLabelMap.ProductFacilityLocation />
        <td>
          <#if facilityLocation?exists>
            <div class="tabletext">
              <b>${uiLabelMap.ProductArea} :</b>&nbsp;${facilityLocation.areaId?if_exists}
              <b>${uiLabelMap.ProductAisle} :</b>&nbsp;${facilityLocation.aisleId?if_exists}
              <b>${uiLabelMap.ProductSection} :</b>&nbsp;${facilityLocation.sectionId?if_exists}
              <b>${uiLabelMap.ProductLevel} :</b>&nbsp;${facilityLocation.levelId?if_exists}
              <b>${uiLabelMap.ProductPosition}:</b>&nbsp;${facilityLocation.positionId?if_exists}
            </div>
          </#if>
          <#if inventoryItem?exists>
            <@inputLookup name="locationSeqId" default=(inventoryItem.locationSeqId)! lookup="LookupFacilityLocation?facilityId=${facilityId!}" form="inventoryItemForm" />
          <#else>
            <@inputLookup name="locationSeqId" default=(locationSeqId)! lookup="LookupFacilityLocation?facilityId=${facilityId!}" form="inventoryItemForm" />
          </#if>
        </td>
      </tr>
      <@inputLookupRow title=uiLabelMap.ProductLotId name="lotId" lookup="LookupLot" form="inventoryItemForm" default=(inventoryItemData.lotId)! />
      <@inputHidden name="uomId" value=(inventoryItemData.uomId)! />
      <@inputHidden name="binNumber" value=(inventoryItemData.binNumber)! />
      <#if (hasSetCostPermission) || !(inventoryItemData.unitCost?has_content) || (inventoryItemData.unitCost == 0)>
        <@inputTextRow title=uiLabelMap.ProductPerUnitPrice name="unitCost" default=(inventoryItemData.unitCost)?default(0) />
      <#else>
        <@inputHidden name="unitCost" value=(inventoryItemData.unitCost)?default(0)/>
      </#if>
      <@inputTextRow title=uiLabelMap.ProductComments name="comments" default=(inventoryItemData.comments)! />
      <#-- accounting tags for this item -->
      <#if tagTypes?has_content>
        <#-- disabled for now until we can chaneg the tag when tag are required to balance in the transaction
             <@accountingTagsSelectRows tags=tagTypes prefix="acctgTagEnumId" entity=inventoryItem /> 
             -->
        <@accountingTagsDisplayRows tags=tagTypes entity=inventoryItem />
      </#if>
      <#if "NON_SERIAL_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
        <@displayRow title=uiLabelMap.ProductAvailablePromiseQuantityHand text="${inventoryItemData.availableToPromiseTotal?if_exists} / ${inventoryItemData.quantityOnHandTotal?if_exists}" />
        <@displayRow title="" text=uiLabelMap.ProductPhysicalInventoryVariance />
      <#elseif "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
        <@inputTextRow title=uiLabelMap.ProductSerialNumber name="serialNumber" default=(inventoryItemData.serialNumber)! />
      <#elseif inventoryItem?exists>
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductSerialAtpQoh />
          <td><div class="tabletext" style="color: red;">${uiLabelMap.ProductErrorType} [${inventoryItem.inventoryItemTypeId?if_exists}] ${uiLabelMap.ProductUnknownSpecifyType}.</div></td>
        </tr>
      </#if>
      <@inputSubmitRow title=uiLabelMap.CommonUpdate />
   </table>
</form>
</@frameSection>
