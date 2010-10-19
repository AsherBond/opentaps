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
--->
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
       <fo:table border-spacing="3pt">
           <fo:table-column column-width="3.75in"/>
          <fo:table-column column-width="3.75in"/>
          <fo:table-body>
            <fo:table-row>    <#-- this part could use some improvement -->
             
             <#-- a special purchased from address for Purchase Orders -->
             <#if orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
             <#if supplierGeneralContactMechValueMap?exists>
               <#assign contactMech = supplierGeneralContactMechValueMap.contactMech>
               <fo:table-cell>
                 <fo:block>
                     ${uiLabelMap.OrderPurchasedFrom}:
                 </fo:block>
                 <#assign postalAddress = supplierGeneralContactMechValueMap.postalAddress>
                 <#if postalAddress?has_content>
                   <#if postalAddress.toName?has_content><fo:block>${postalAddress.toName}</fo:block></#if>
                   <#if postalAddress.attnName?has_content><fo:block>${postalAddress.attnName?if_exists}</fo:block></#if>
                   <fo:block>${postalAddress.address1?if_exists}</fo:block>
                   <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
                   <fo:block>${postalAddress.city?if_exists}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if><#if postalAddress.postalCode?has_content>${postalAddress.postalCode}</#if></fo:block>
                   <fo:block>${postalAddress.countryGeoId?if_exists}</fo:block>
                 </#if>
               </fo:table-cell>
             <#else>
               <#-- here we just display the name of the vendor, since there is no address -->
               <fo:table-cell>
                 <#assign vendorParty = orderReadHelper.getBillFromParty()>
                 <fo:block>
                   <fo:inline font-weight="bold">${uiLabelMap.OrderPurchasedFrom}:</fo:inline> ${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(vendorParty)}
                 </fo:block>
               </fo:table-cell> 
             </#if>
             </#if>
             
             <#-- list all postal addresses of the order.  there should be just a billing and a shipping here. -->
             <#list orderContactMechValueMaps as orderContactMechValueMap>
               <#assign contactMech = orderContactMechValueMap.contactMech>
               <#assign contactMechPurpose = orderContactMechValueMap.contactMechPurposeType>
               <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
               <#assign postalAddress = orderContactMechValueMap.postalAddress>
               <fo:table-cell>
                 <fo:block font-weight="bold">${contactMechPurpose.get("description",locale)}:</fo:block>
                 <#if postalAddress?has_content>
                   <#if postalAddress.toName?has_content><fo:block>${postalAddress.toName?if_exists}</fo:block></#if>
                   <#if postalAddress.attnName?has_content><fo:block>${postalAddress.attnName?if_exists}</fo:block></#if>
                   <fo:block>${postalAddress.address1?if_exists}</fo:block>
                   <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
                   <fo:block>${postalAddress.city?if_exists}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if><#if postalAddress.postalCode?has_content>${postalAddress.postalCode}</#if></fo:block>
                 </#if>
               </fo:table-cell>
               </#if>
             </#list>
            </fo:table-row>
         </fo:table-body>
       </fo:table>

       <fo:block white-space-collapse="false"> </fo:block> 

<fo:block space-after="10pt"/>

<#if orderHeader?has_content>
  <fo:table table-layout="fixed">

    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(3)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
  
    <fo:table-header>
      <fo:table-row font-weight="bold">
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid"><fo:block>Product ID</fo:block></fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid"><fo:block>UPC</fo:block></fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid"><fo:block>${uiLabelMap.ProductInternalName}</fo:block></fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid"><fo:block text-align="right">${uiLabelMap.ProductQtyReceived}</fo:block></fo:table-cell>
      </fo:table-row>        
    </fo:table-header>
        
    <fo:table-body>
      <#list productDatas?sort_by("productInternalName") as data>
        <#if ((data_index % 2) == 0)>
          <#assign rowColor = "white">
        <#else>
          <#assign rowColor = "#CCCCCC">
        </#if>
        <fo:table-row>
          <fo:table-cell background-color="${rowColor}">
            <fo:block>${data.productId?if_exists}</fo:block>
          </fo:table-cell>
          <fo:table-cell background-color="${rowColor}">
            <fo:block>${data.upca ! uiLabelMap.CommonNA}</fo:block>
          </fo:table-cell>
          <fo:table-cell background-color="${rowColor}">
            <fo:block>${data.productInternalName!}</fo:block>
          </fo:table-cell>
          <fo:table-cell background-color="${rowColor}">
            <fo:block text-align="right">${data.quantityReceived}</fo:block>
          </fo:table-cell>
        </fo:table-row>

        <#if data.fulfilledOrderDatas?has_content>
        <fo:table-row font-weight="bold" font-size="12pt" font-style="italic">
            <fo:table-cell background-color="${rowColor}"><fo:block/></fo:table-cell>
            <fo:table-cell background-color="${rowColor}"><fo:block/></fo:table-cell>
            <fo:table-cell background-color="${rowColor}" padding-left="30px" padding-right="30px">
              <fo:table>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-body>
                  <fo:table-row>
                    <fo:table-cell background-color="${rowColor}"><fo:block>Fulfilled Back Orders</fo:block></fo:table-cell>
                    <fo:table-cell background-color="${rowColor}" text-align="right"><fo:block>Backordered</fo:block></fo:table-cell>
                  </fo:table-row>

                  <#list data.fulfilledOrderDatas as fulfilledOrderData>
                  <fo:table-row>
                    <fo:table-cell background-color="${rowColor}">
                      <fo:block>${fulfilledOrderData.salesOrderId!}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell background-color="${rowColor}">
                      <fo:block text-align="right">${fulfilledOrderData.quantityFulfilled}</fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                </#list>

                </fo:table-body>
              </fo:table>
            </fo:table-cell>
            <fo:table-cell background-color="${rowColor}"><fo:block/></fo:table-cell>
          </fo:table-row>
        </#if>
      </#list>
    </fo:table-body>
  </fo:table>    
</#if>

</#escape>
