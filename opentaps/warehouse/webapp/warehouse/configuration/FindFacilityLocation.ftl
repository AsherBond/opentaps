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
  <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.ProductNewFacilityLocation}</a>
</#assign>

<@frameSection title=uiLabelMap.ProductFindLocation extra=extraOptions>
  <form action="<@ofbizUrl>FindFacilityLocation</@ofbizUrl>" method="POST" name="findFacilityLocation">
    <table class="twoColumnForm">
      <@inputHidden name="look_up" value="true"/>
      <@inputHidden name="facilityId" value="${facilityId}"/>
      <@inputLookupRow title=uiLabelMap.ProductLocationSeqId name="locationSeqId" lookup="LookupFacilityLocation?facilityId=${facilityId}" form="findFacilityLocation" />
      <@inputTextRow title=uiLabelMap.CommonArea name="areaId" />
      <@inputTextRow title=uiLabelMap.ProductAisle name="aisleId" />
      <@inputTextRow title=uiLabelMap.ProductSection name="sectionId" />
      <@inputTextRow title=uiLabelMap.ProductLevel name="levelId" />
      <@inputTextRow title=uiLabelMap.ProductPosition name="positionId" />
      <@inputSubmitRow title=uiLabelMap.CommonFind />
    </table>
  </form>
</@frameSection>

<#if foundLocations?exists>
  <@frameSection title="${uiLabelMap.CommonFound}:&nbsp;${foundLocations.size()}&nbsp;${uiLabelMap.ProductLocations}">
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.ProductFacility}</td>
        <td>${uiLabelMap.ProductLocationSeqId}</td>
        <td>${uiLabelMap.ProductType}</td>
        <td>${uiLabelMap.CommonArea}</td>
        <td>${uiLabelMap.ProductAisle}</td>
        <td>${uiLabelMap.ProductSection}</td>
        <td>${uiLabelMap.ProductLevel}</td>
        <td>${uiLabelMap.ProductPosition}</td>
        <td>&nbsp;</td>
      </tr>
      <#assign rowClass = "2"/>
      <#list foundLocations as location>
        <#assign locationTypeEnum = location.getRelatedOneCache("TypeEnumeration")?if_exists/>
        <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
          <td><a href="<@ofbizUrl>EditFacility?facilityId=${(location.facilityId)?if_exists}</@ofbizUrl>">${(location.facilityId)?if_exists}</a></td>
          <td><a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>">${(location.locationSeqId)?if_exists}</a></td>
          <td>${(locationTypeEnum.get("description",locale))?default(location.locationTypeEnumId?if_exists)}</td>
          <td>${(location.areaId)?if_exists}</td>
          <td>${(location.aisleId)?if_exists}</td>
          <td>${(location.sectionId)?if_exists}</td>
          <td>${(location.levelId)?if_exists}</td>
          <td>${(location.positionId)?if_exists}</td>
          <td class="button-col">
            <a href="<@ofbizUrl>findInventoryItem?&locationSeqId=${(location.locationSeqId)?if_exists}&performFind=Y</@ofbizUrl>">${uiLabelMap.WarehouseFindInventoryItem}</a>
            <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${(location.facilityId)?if_exists}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
          </td>
        </tr>
        <#if rowClass == "2">
          <#assign rowClass = "1"/>
        <#else>
          <#assign rowClass = "2"/>
        </#if>
      </#list>
    </table>
  </@frameSection>
</#if>
