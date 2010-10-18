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
        
<#-- 
Ship group summary for order confirmation.  Lists each ship group, its 
destination address, products and quantities associated with it,
and similar information.  This is designed to be tacked on to the 
standard order confirmation page and to be re-usable by other screens.
-->

<#if !(cart?exists)><#assign cart = shoppingCart?if_exists/></#if>

<#if cart?exists>
<div class="screenlet">
  <div class="screenlet-header">
    <div class="boxhead">&nbsp;${uiLabelMap.OrderShippingInformation}</div>
  </div>
  <div class="screenlet-body">
    <table width="100%">

      <#-- header -->

      <tr>
        <td><span class="tableheadtext">${uiLabelMap.OrderDestination}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.CrmDropShippedFrom}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductShipmentMethod}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductItem}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductQuantity}</span></td>
      </tr>


      <#-- BEGIN LIST SHIP GROUPS -->
      <#-- 
      The structure of this table is one row per line item, grouped by ship group.  
      The address column spans a number of rows equal to the number of items of its group.  
      -->

      <#assign shipGroups = cart.getShipGroups()>
      <#list shipGroups as cartShipInfo>
      <#assign shipGroupSeqId = shipGroups.indexOf(cartShipInfo)/>
      <#assign numberOfItems = cartShipInfo.getShipItems().size()>
      <#if (numberOfItems > 0)>

      <#-- spacer goes here -->
      <tr><td colspan="5"><hr class="sepbar"/></td></tr>

      <tr>

        <#-- address destination column (spans a number of rows = number of cart items in it) -->

        <td rowspan="${numberOfItems}" class="tabletext">
          <#assign contactMech = delegator.findByPrimaryKey("ContactMech", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechId", cartShipInfo.contactMechId))?if_exists />
          <#if contactMech?has_content>
            <#assign address = contactMech.getRelatedOne("PostalAddress")?if_exists />
          </#if>
          
          <#if cartShipInfo.contactMechId?exists>
            <#if "_NA_" == cartShipInfo.contactMechId>
              ${uiLabelMap.CrmAddressUnknown}
            <#else>
              <#if address?exists>
                <#if address.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${address.toName}<br/></#if>
                <#if address.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b>&nbsp;${address.attnName}<br/></#if>
                <#if address.address1?has_content>${address.address1}<br/></#if>
                <#if address.address2?has_content>${address.address2}<br/></#if>
                <#if address.city?has_content>${address.city}</#if>
                <#if address.stateProvinceGeoId?has_content>&nbsp;${address.stateProvinceGeoId}</#if>
                <#if address.postalCode?has_content>, ${address.postalCode?if_exists}</#if>
                <#if address.countryGeoId?has_content ><br/><@displayGeo geoId=address.countryGeoId /></#if>
              </#if>
            </#if>
          <#else>
            ${uiLabelMap.CommonNoAddress}
          </#if>
          
        </td>

        <#-- supplier id (for drop shipments) (also spans rows = number of items) -->

        <td rowspan="${numberOfItems}" valign="top" class="tabletext">
          <#assign supplier =  delegator.findByPrimaryKey("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", cartShipInfo.getSupplierPartyId()))?if_exists />
          <#if supplier?has_content>${supplier.groupName?default("")} (supplier.partyId)</#if>
        </td>

        <#-- carrier column (also spans rows = number of items) -->

        <td rowspan="${numberOfItems}" valign="top" class="tabletext">
          <#assign carrier =  delegator.findByPrimaryKey("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", cartShipInfo.getCarrierPartyId()))?if_exists />
          <#assign method =  delegator.findByPrimaryKey("ShipmentMethodType", Static["org.ofbiz.base.util.UtilMisc"].toMap("shipmentMethodTypeId", cartShipInfo.getShipmentMethodTypeId()))?if_exists />
          <#if carrier?has_content>${carrier.groupName?default(carrier.partyId)}</#if>
          <#if method?has_content>${method.description?default(method.shipmentMethodTypeId)}</#if>
          <#if cart.getThirdPartyAccountNumber(shipGroupSeqId)?has_content>
             <br/>
             ${uiLabelMap.CrmOrderThirdPartyAccountNo}: ${cart.getThirdPartyAccountNumber(shipGroupSeqId)?default("")}<br/>
             ${uiLabelMap.CrmZipCode}: ${cart.getThirdPartyPostalCode(shipGroupSeqId)?default("")} ${uiLabelMap.CommonCountry}: ${cart.getThirdPartyCountryCode(shipGroupSeqId)?default("")}
          </#if> 
        </td>

        <#-- list each ShoppingCartItem in this group -->

        <#assign itemIndex = 0 />
        <#list cartShipInfo.getShipItems() as shoppingCartItem>
        <#if (itemIndex > 0)> <tr> </#if>

        <td valign="top" class="tabletext"> ${shoppingCartItem.getProductId()?default("")} - ${shoppingCartItem.getName()?default("")} </td>
        <td valign="top" class="tabletext"> ${cartShipInfo.getShipItemInfo(shoppingCartItem).getItemQuantity()?default("0")} </td>
        <#assign itemIndex = itemIndex + 1 />
        <#if (itemIndex == 0)> </tr> </#if>
        </#list>

      </tr>

      </#if>
      </#list>

      <#-- END LIST SHIP GROUPS -->

    </table>
  </div>
</div>
</#if>
