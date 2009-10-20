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

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- if there is no product then force a product lookup -->
<#if !(product?has_content)>
  <form method="post" action="<@ofbizUrl>receiveInventoryItem</@ofbizUrl>" name="productForm" style="margin: 0;">
    <table>
      <@inputAutoCompleteProductRow name="productId" title="${uiLabelMap.ProductProductId}" />
      <#assign defaultInvItemTypeId = facility.defaultInventoryItemTypeId?default("NON_SERIAL_INV_ITEM")/>
      <@inputSelectRow title="${uiLabelMap.ProductInventoryItemType}" name="inventoryItemTypeId" list=inventoryItemTypes displayField="description" default="${defaultInvItemTypeId}"/>
      <@inputSubmitRow title="${uiLabelMap.ProductFindProduct}"/>
    </table>
  </form>
<#-- otherwise show the screen for receiving product -->
<#else>

  <form method="post" action="<@ofbizUrl>receiveSingleInventoryProduct</@ofbizUrl>" name="selectAllForm" style="margin: 0;">
    <@inputHidden name="validateAccountingTags" value="True"/>
    <table border="0" cellpadding="2" cellspacing="0">
      <#if backOrderedItems?has_content>
      <tr>
        <td width="14%">&nbsp;</td>
        <td colspan="3" align="left">
          <div style="background-color: #EEE">      
            <span class="requiredField"/>${uiLabelMap.WarehouseBackOrderedProductIsBackOrdered}</span>
            <table width="100%">
              <tr>
                <th class="tableheadtext" width="35%">${uiLabelMap.OpentapsReservationSequence}</th>
                <th class="tableheadtext" width="35%">${uiLabelMap.OrderOrder}</th>
                <th class="tableheadtext" width="15%">${uiLabelMap.OrderOrderQty}</th>
                <th class="tableheadtext" width="15%">${uiLabelMap.OpentapsQtyBackOrdered}</th>
              </tr>
              <#list backOrderedItems as backOrderedItem>
                <tr>
                  <td class="tabletext" align="center">${getLocalizedDate(backOrderedItem.reservedDatetime)} <#if backOrderedItem.sequenceId?has_content>: ${backOrderedItem.sequenceId}</#if></td>
                  <td class="tabletext" align="center">${backOrderedItem.orderId} : ${backOrderedItem.orderItemSeqId}</td>
                  <td class="tabletext" align="center">${backOrderedItem.quantity}</td>
                  <td class="requiredField" align="center">${backOrderedItem.quantityNotAvailable}</td>
                </tr>
              </#list>
            </table>
          </div>      
        </td>
      </tr>
      <tr><td colspan="4">&nbsp;</td></tr>
      </#if>
      <#-- general request fields -->
      <@inputHidden name="facilityId" value=facilityId! />
      <#-- special service fields -->
      <@inputHidden name="productId" value=(product.productId)! />
      <@inputHidden name="inventoryItemTypeId" value=inventoryItemTypeId! />
      <@displayRow title=uiLabelMap.ProductProduct text="<b>${product.internalName?default('No Internal Name')}</b> (${product.productId?if_exists})" />
      <#list goodIdentifications as goodIdentification>
        <@displayRow title=goodIdentification.goodIdentificationTypeId text=goodIdentification.idValue! />
      </#list>
      <@inputTextRow title=uiLabelMap.ProductItemDescription name="itemDescription" />
      <#-- Comment this back in when we figure out how to do 3-rd party owner, probably configured
      <@inputLookupRow title=uiLabelMap.ProductFacilityOwner name="ownerPartyId" lookup="LookupPartyName" form="selectAllForm" />
      -->
      <@inputDateTimeRow title=uiLabelMap.ProductDateReceived name="datetimeReceived" default=getLocalizedDate(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()) />
      <#-- facility location(s) -->
      <#assign facilityLocations = (product.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists/>
      <#if facilityLocations?has_content>
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductFacilityLocation />
          <td>
            <select name="locationSeqId" class="inputBox" >
              <#list facilityLocations as productFacilityLocation>
                <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")/>
                <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists/>
                <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists/>
                <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
              </#list>
              <option value="">${uiLabelMap.ProductNoLocation}</option>
            </select>
          </td>
        </tr>
      <#else>
        <@inputLookupRow title=uiLabelMap.ProductFacilityLocation name="locationSeqId" lookup="LookupFacilityLocation?facilityId=${facilityId!}" form="selectAllForm" />
      </#if>

      <#if (inventoryItemTypeId == "SERIALIZED_INV_ITEM")>
        <@inputTextRow title=uiLabelMap.ProductSerialNumber name="serialNumber" />
        <@inputHidden name="quantityAccepted" value="1"/>
      <#else>
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductLotId />
          <td>
            <@inputLookup name="lotId" lookup="LookupLot" form="selectAllForm" />
            <a href="javascript:call_fieldlookup2(document.selectAllForm.lotId,'createLotPopupForm');" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
          </td>
        </tr>
        <@inputTextRow title=uiLabelMap.ProductQuantityAccepted name="quantityAccepted" default=defaultQuantity?default(1)?string.number size=10 />
      </#if>

      <#-- it does not seem this works, so I'm commenting it out.  Comment it back if it ever works in OFBIZ.  SC
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductQuantityRejected />
          <td>
            <@inputTextCell name="quantityRejected" size=5 />
            <select name="rejectionId" size="1" class="selectBox">
              <option></option>
              <#list rejectReasons as nextRejection>
                <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
              </#list>
            </select>
          </td>
        </tr>
      -->
      <@inputHidden name="quantityRejected" value="0"/>

      <#-- how best to handle unit costs and override permission?  if there is no unit cost, should any user be allowed to set it?
      For now, we'll do it that way for easier testing, but perhaps it's better changed for deployments.  Note the
       getProductCosts service will return a 0 if there is no unit cost.  -->
      <#if (hasSetCostPermission) || !(unitCost?has_content) || (unitCost == 0)>
        <@inputTextRow title=uiLabelMap.ProductPerUnitPrice name="unitCost" default=unitCost! size=10 />
      <#else>
        <@inputHidden name="unitCost" value=unitCost!/>
      </#if>

      <#-- accounting tags -->
      <#if acctgTags?has_content>
        <@accountingTagsSelectRows tags=acctgTags prefix="acctgTagEnumId" />
      </#if>

      <@inputSubmitRow title=uiLabelMap.CommonReceive />
    </table>
    <script type="text/javascript">
      document.selectAllForm.quantityAccepted.focus();
    </script>
  </form>
</#if>
