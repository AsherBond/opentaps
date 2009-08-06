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

<#escape x as x?xml>
<#macro pickInfoDetail pickQuantity picklistBinInfoList product facilityLocation="" facilityLocationInfo="">
    <fo:table-row>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <#if (facilityLocation?has_content) && (facilityLocation?is_hash)>
                <fo:block>${facilityLocation.areaId?if_exists}-${facilityLocation.aisleId?if_exists}-${facilityLocation.sectionId?if_exists}-${facilityLocation.levelId?if_exists}-${facilityLocation.positionId?if_exists}</fo:block>
            <#else>
                <fo:block>[${uiLabelMap.ProductNoLocation}]</fo:block>
            </#if>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <#if product?has_content>
                <fo:block>${product.internalName?default("Internal Name Not Set!")} [${product.productId}]</fo:block>
            <#else>
                <fo:block> </fo:block>
            </#if>
            <#if (facilityLocationInfo?has_content) && (facilityLocationInfo?is_hash) && (facilityLocationInfo.message)?has_content>
                <fo:block>${facilityLocationInfo.message?if_exists}</fo:block>
            </#if>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <fo:block>${pickQuantity}</fo:block>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <#list picklistBinInfoList as picklistBinInfo>
                <fo:block>${picklistBinInfo.quantity} ${uiLabelMap.CommonTo} #${picklistBinInfo.picklistBin.binLocationNumber}</fo:block>
            </#list>
        </fo:table-cell>
    </fo:table-row>
    <#-- toggle the row color -->
    <#if rowColor == "white">
        <#assign rowColor = "#D4D0C8">
    <#else>
        <#assign rowColor = "white">
    </#if>
</#macro>

<#macro picklistItemInfoDetail picklistItemInfo product facilityLocation>
    <#local picklistItem = picklistItemInfo.picklistItem>
    <#local orderItem = picklistItemInfo.orderItem>
    <fo:table-row>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <#if facilityLocation?has_content>
                <fo:block>${facilityLocation.areaId?if_exists}-${facilityLocation.aisleId?if_exists}-${facilityLocation.sectionId?if_exists}-${facilityLocation.levelId?if_exists}-${facilityLocation.positionId?if_exists}</fo:block>
            <#else>
                <fo:block>[${uiLabelMap.ProductNoLocation}]</fo:block>
            </#if>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <#if product?has_content>
                <fo:block>${product.internalName?default("Internal Name Not Set!")} [${product.productId}]</fo:block>
            <#else>
                <fo:block> </fo:block>
            </#if>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <fo:block>${picklistItem.quantity}</fo:block>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <fo:block>${orderItemShipGrpInvRes.orderId}:${orderItem.orderItemSeqId}</fo:block>
        </fo:table-cell>
        <fo:table-cell padding="2pt" background-color="${rowColor}">
            <fo:block>
                ${picklistItemInfo.inventoryItemAndLocation.inventoryItemId}<#if picklistItemInfo.inventoryItemAndLocation.binNumber?exists>:${picklistItemInfo.inventoryItemAndLocation.binNumber}</#if>
            </fo:block>
        </fo:table-cell>
    </fo:table-row>
    <#-- toggle the row color -->
    <#if rowColor == "white">
        <#assign rowColor = "#D4D0C8">
    <#else>
        <#assign rowColor = "white">
    </#if>
</#macro>
</#escape>