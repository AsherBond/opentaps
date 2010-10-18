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
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#assign extraOptions>
  <#if facilityId?exists && locationSeqId?exists>
    <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacilityLocation}</a>
    <a href="<@ofbizUrl>findInventoryItem?locationSeqId=${locationSeqId}&performFind=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.WarehouseFindInventoryItem}</a>
  </#if>
</#assign>

<#assign title=uiLabelMap.ProductNewFacilityLocation/>
<#if facilityLocation?exists>
  <#assign title=uiLabelMap.ProductFacilityLocation/>
</#if>

<@frameSection title=title extra=extraOptions>

  <#if facilityId?exists && !(facilityLocation?exists)>
    <form action="<@ofbizUrl>CreateFacilityLocation</@ofbizUrl>" method="post">
      <@inputHidden name="facilityId" value="${facilityId}"/>
      <table class="twoColumnForm">
  <#elseif facilityLocation?exists>
    <form action="<@ofbizUrl>UpdateFacilityLocation</@ofbizUrl>" method="post">
      <@inputHidden name="facilityId" value="${facilityId!}"/>
      <@inputHidden name="locationSeqId" value="${locationSeqId}"/>
      <table class="twoColumnForm">
        <@displayRow title=uiLabelMap.ProductFacilityId text=facilityId! />
        <@displayRow title=uiLabelMap.ProductLocationSeqId text=locationSeqId! />
  <#else>
    <p>${uiLabelMap.ProductNotCreateLocationFacilityId}</p>
  </#if>

      <@inputSelectRow title=uiLabelMap.ProductType name="locationTypeEnumId" list=locationTypeEnums key="enumId" displayField="description" />
      <@inputTextRow title=uiLabelMap.CommonArea name="areaId" default=(facilityLocation.areaId)! maxlength=20 />
      <@inputTextRow title=uiLabelMap.ProductAisle name="aisleId" default=(facilityLocation.aisleId)! maxlength=20 />
      <@inputTextRow title=uiLabelMap.ProductSection name="sectionId" default=(facilityLocation.sectionId)! maxlength=20 />
      <@inputTextRow title=uiLabelMap.ProductLevel name="levelId" default=(facilityLocation.levelId)! maxlength=20 />
      <@inputTextRow title=uiLabelMap.ProductPosition name="positionId" default=(facilityLocation.positionId)! maxlength=20 />

      <#if locationSeqId?exists>
        <@inputSubmitRow title=uiLabelMap.CommonUpdate />
      <#else>
        <@inputSubmitRow title=uiLabelMap.CommonSave />
      </#if>
    </table>
  </form>

</@frameSection>

<#if locationSeqId?exists>
  <@frameSection title=uiLabelMap.ProductLocationProduct>
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.ProductMinimumStockAndMoveQuantity}</td>
      </tr>
      <#list productFacilityLocations?if_exists as productFacilityLocation>
        <#assign product = productFacilityLocation.getRelatedOne("Product")?if_exists/>
        <tr>
          <td><#if product?exists>${(product.internalName)?if_exists}</#if> [${productFacilityLocation.productId}]</td>
          <td>
            <form method="post" action="<@ofbizUrl>updateProductFacilityLocation</@ofbizUrl>" id="lineForm${productFacilityLocation_index}">
              <@inputHidden name="productId" value="${(productFacilityLocation.productId)?if_exists}"/>
              <@inputHidden name="facilityId" value="${(productFacilityLocation.facilityId)?if_exists}"/>
              <@inputHidden name="locationSeqId" value="${(productFacilityLocation.locationSeqId)?if_exists}"/>
              <@inputText size="10" name="minimumStock" default="${(productFacilityLocation.minimumStock)?if_exists}"/>
              <@inputText size="10" name="moveQuantity" default="${(productFacilityLocation.moveQuantity)?if_exists}"/>
              <@inputSubmit title=uiLabelMap.CommonUpdate />
              <a href="javascript:$('lineForm${productFacilityLocation_index}').action='<@ofbizUrl>deleteProductFacilityLocation</@ofbizUrl>';$('lineForm${productFacilityLocation_index}').submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
            </form>
          </td>
        </tr>
      </#list>
    </table>
  </@frameSection>

  <@frameSection title=uiLabelMap.ProductAddProduct>
    <form method="post" action="<@ofbizUrl>createProductFacilityLocation</@ofbizUrl>" style="margin: 0;" name="createProductFacilityLocationForm">
      <@inputHidden name="facilityId" value="${facilityId?if_exists}"/>
      <@inputHidden name="locationSeqId" value="${locationSeqId?if_exists}"/>
      <@inputHidden name="useValues" value="true"/>
      <@display text=uiLabelMap.ProductProductId /><@inputAutoCompleteProduct name="productId" />
      <@display text=uiLabelMap.ProductMinimumStock /><@inputText size="10" name="minimumStock"/>
      <@display text=uiLabelMap.ProductMoveQuantity /><@inputText size="10" name="moveQuantity"/>
      <@inputSubmit title=uiLabelMap.CommonAdd />
    </form>
  </@frameSection>

</#if>

