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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if shipmentPackageDatas?has_content>
  <br/>
  <table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductPackage}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonCreated}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list shipmentPackageDatas as shipmentPackageData>
      <#assign shipmentPackage = shipmentPackageData.shipmentPackage>
      <#assign shipmentPackageContents = shipmentPackageData.shipmentPackageContents?if_exists>
      <#assign shipmentPackageRouteSegs = shipmentPackageData.shipmentPackageRouteSegs?if_exists>
      <#assign weightUom = shipmentPackageData.weightUom?if_exists>
      <tr>
        <@displayCell text=shipmentPackage.shipmentPackageSeqId />
        <@displayDateCell date=shipmentPackage.dateCreated />
        <@displayCell text="${uiLabelMap.ProductWeight} : ${shipmentPackage.weight?if_exists}" />
        <td><@display text="${uiLabelMap.ProductWeightUnit} :"/><#if weightUom?has_content><@display text=weightUom.get("description",locale)/><#else><@display text=shipmentPackage.weightUomId?if_exists/></#if></td>
      </tr>
      <#list shipmentPackageContents as shipmentPackageContent>
        <tr>
          <td><span class="tabletext">&nbsp;</span></td>
          <@displayCell text="${uiLabelMap.ProductItem} :${shipmentPackageContent.shipmentItemSeqId}" />
          <@displayCell text="${uiLabelMap.ProductQuantity} :${shipmentPackageContent.quantity?if_exists}" />
          <td><span class="tabletext">&nbsp;</div></td>
        </tr>
      </#list>
      <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <tr>
          <td><span class="tabletext">&nbsp;</span></td>
          <@displayCell text="${uiLabelMap.ProductRouteSegment} :${shipmentPackageRouteSeg.shipmentRouteSegmentId}" />
          <@displayCell text="${uiLabelMap.ProductTracking} : ${shipmentPackageRouteSeg.trackingCode?if_exists}" />
          <@displayCell text="${uiLabelMap.ProductBox} : ${shipmentPackageRouteSeg.boxNumber?if_exists}" />
        </tr>
      </#list>
    </#list>
  </table>
</#if>
