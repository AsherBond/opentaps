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
<@import location="component://opentaps-common/webapp/common/order/infoMacros.ftl"/>

<#if order?exists>

<#if order.shipGroups?has_content>
  <script type="text/javascript">
  /*<![CDATA[*/
    function showOrHideEditAddressButton(/*String*/ shipGroupSeqId) {
      // get the button
      var editAddressButton = document.getElementById('editContactMechButton_' + shipGroupSeqId);
      if (! editAddressButton) return;
      // get selected contact mech
      var contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);
      var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
      var hideButton = false;
      // do not show edit button for no address or unknown address
      if ("" == contactMechId || "_NA_" == contactMechId) hideButton = true;
      // hide or show the button
      if (hideButton) {
        editAddressButton.style.visibility = 'hidden';
      } else {
        editAddressButton.style.visibility = 'visible';
      }
    }

    function editCurrentAddress(/*String*/ shipGroupSeqId, /*String*/ oldContactMechId) {
      var url = "<@ofbizUrl>orderViewEditContactMech?partyId=${order.mainExternalParty.partyId}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=orderview%3ForderId%3D${order.orderId}&orderId=${order.orderId}</@ofbizUrl>";
      // get selected contact mech
      var contactMechIdInput = document.getElementById('contactMechId_' + shipGroupSeqId);
      var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
      if ("" == contactMechId || "_NA_" == contactMechId) return;
      url += "&contactMechId=" + contactMechId + "&shipGroupSeqId=" + shipGroupSeqId + "&oldContactMechId=" + oldContactMechId;
      location.href = url;
    }

    function switchToShipGroup() {
      var selector = document.getElementById('shipGroupSelect');
      if (! selector) return;

      var toShipGroupSeqId = selector.options[selector.selectedIndex].value;
      var toElement = document.getElementById('shipGroupScreenlet-' + toShipGroupSeqId);
      if (! toElement) return;

      <#list order.shipGroups as sg>
      el = document.getElementById('shipGroupScreenlet-${sg.shipGroupSeqId}');
      if (el != null)  opentaps.addClass(el, 'hidden');
      </#list>

      opentaps.removeClass(toElement, 'hidden');
    }

    function splitShipGroup() {
      var selector = document.getElementById('shipGroupSelect');
      var url = "<@ofbizUrl>newShipGroup?orderId=${order.orderId}</@ofbizUrl>";
      if (selector) {
        var shipGroupSeqId = selector.options[selector.selectedIndex].value;
        if ("" == shipGroupSeqId) return false;
        url += "&shipGroupSeqId=" + shipGroupSeqId;
      }
      location.href = url;
      return false;
    }
  /*]]>*/
  </script>

  <#assign sectionTitle>
    <#if order.shipGroups?size gt 1>
      <select id="shipGroupSelect" class="inputBox" onChange="switchToShipGroup()" onkeyup="switchToShipGroup()">
        <#list order.shipGroups as sg>
          <option value="${sg.shipGroupSeqId}" ${(parameters.shipGroupSeqId?exists && sg.shipGroupSeqId == parameters.shipGroupSeqId)?string('selected="selected"','')}>
            <@expandLabel label="CrmOrderShipGroupTo" params={"shipGroupSeqId", sg.shipGroupSeqId} />
            <#if sg.hasUnknownPostalAddress()>${uiLabelMap.CrmAddressUnknown}<#else/>${(sg.postalAddress.address1)?default("")} - ${sg.postalAddress.city?default("")}</#if>
          </option>
        </#list>
      </select>
    <#else>
      <#assign sg = order.shipGroups[0] />
      <@expandLabel label="CrmOrderShipGroupTo" params={"shipGroupSeqId", sg.shipGroupSeqId} />
      <#if sg.hasUnknownPostalAddress()>${uiLabelMap.CrmAddressUnknown}<#else/>${(sg.postalAddress.address1)?default("")} - ${sg.postalAddress.city?default("")}</#if>
    </#if>
    <#if order.shipGroups?size gt 1>
      <a href="#" onClick="return splitShipGroup()" class="buttontext">${uiLabelMap.CrmOrderSplitShipGroup}</a>
    <#else>
      <@displayLink href="newShipGroup?orderId=${order.orderId}&amp;shipGroupSeqId=${order.shipGroups[0].shipGroupSeqId}" text=uiLabelMap.CrmOrderSplitShipGroup class="buttontext" />
    </#if>
  </#assign>
  
  <@frameSection title=sectionTitle>

    <#assign firstShipGroup = true />
    <#list order.shipGroups as shipGroup>
      <div class="screenlet-body${firstShipGroup?string('',' hidden')}" id="shipGroupScreenlet-${shipGroup.shipGroupSeqId}">
        <#assign firstShipGroup = false />
        <form name="updateOrderItemShipGroup-${shipGroup.shipGroupSeqId}" method="post" action="<@ofbizUrl>updateOrderItemShipGroup</@ofbizUrl>">
          <@inputHidden name="orderId" value="${order.orderId?if_exists}" />
          <@inputHidden name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId?if_exists}" />
          <@inputHidden name="contactMechPurposeTypeId" value="SHIPPING_LOCATION" />
          <@inputHidden name="oldContactMechId" value="${shipGroup.contactMechId?if_exists}" />
          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            
            <@infoRowNested title=uiLabelMap.OrderAddress>
              <div class="tabletext">
                <#if !order.isCancelled() && !order.isCompleted() && !order.isRejected()>
                  <select id="contactMechId_${shipGroup.shipGroupSeqId}" name="contactMechId" class="selectBox" onChange="showOrHideEditAddressButton('${shipGroup.shipGroupSeqId}')">
                    <#if shipGroup.contactMechId?has_content>
                      <option selected="selected" value="${shipGroup.contactMechId}">
                        <#if shipGroup.hasUnknownPostalAddress()>${uiLabelMap.CrmAddressUnknown}<#else/>${(shipGroup.postalAddress.address1)?default("")} - ${shipGroup.postalAddress.city?default("")}</#if>
                      </option>
                    <#else/>
                      <option selected="selected" value="">${uiLabelMap.CommonNoAddress}</option>
                    </#if>
                    <#if order.mainExternalParty.shippingAddresses?has_content>
                      <option disabled="disabled" value=""></option>
                      <option value="_NA_">${uiLabelMap.CrmAddressUnknown}</option>
                      <#list order.mainExternalParty.shippingAddresses as shippingPostalAddress>
                        <option value="${shippingPostalAddress.contactMechId}">${(shippingPostalAddress.address1)?default("")} - ${shippingPostalAddress.city?default("")}</option>
                      </#list>
                    </#if>
                  </select>
                  <a class="buttontext" href="<@ofbizUrl>orderViewEditContactMech?partyId=${order.mainExternalParty.partyId}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=orderview%3ForderId%3D${order.orderId}&orderId=${order.orderId}&shipGroupSeqId=${shipGroup.shipGroupSeqId?if_exists}&oldContactMechId=${shipGroup.contactMechId?if_exists}</@ofbizUrl>">${uiLabelMap.CommonNew}</a>
                  <a id="editContactMechButton_${shipGroup.shipGroupSeqId}" class="buttontext" href="#" onClick="editCurrentAddress('${shipGroup.shipGroupSeqId?if_exists}', '${shipGroup.contactMechId?if_exists}');">${uiLabelMap.CommonEdit}</a> <#if "_NA_" == shipGroup.contactMechId><div class="errorMessage">(${uiLabelMap.CrmPleaseSetShippingAddress})</div></#if>
                <#else/>
                  ${(shipGroup.postalAddress.address1)?default("")} - ${shipGroup.postalAddress.city?default("")}
                </#if>
              </div>
            </@infoRowNested>
            
            <script type="text/javascript">
              showOrHideEditAddressButton('${shipGroup.shipGroupSeqId?if_exists}');
            </script>

            <#if shipGroup.shipmentMethodTypeId?has_content>
              <@infoRowNested title=uiLabelMap.CommonMethod>
                <#if shipGroup.carrierPartyId?has_content || shipmentMethodType?has_content>
                  <div class="tabletext">
                    <#if !order.isCancelled() && !order.isCompleted() && !order.isRejected()>
                      <#-- 
                         passing the shipmentMethod value as the combination of two fields value
                         i.e shipmentMethodTypeId & carrierPartyId and this two field values are separated bye
                         "@" symbol. 
                      -->
                      <select name="shipmentMethod" class="selectBox">
                        <option value="${shipGroup.shipmentMethodTypeId}@${shipGroup.carrierPartyId?if_exists}"><#if shipGroup.carrierPartyId != "_NA_">${shipGroup.carrierPartyId?if_exists}</#if>&nbsp;${shipGroup.shipmentMethodType.get("description",locale)?default("")}</option>
                        <#list order.productStore.productStoreShipmentMethViews as productStoreShipmentMethod>
                          <#if productStoreShipmentMethod.partyId?has_content || productStoreShipmentMethod?has_content>
                            <#assign shipmentMethodTypeAndParty = productStoreShipmentMethod.shipmentMethodTypeId + "@" + productStoreShipmentMethod.partyId/>
                            <option value="${shipmentMethodTypeAndParty}"><#if productStoreShipmentMethod.partyId != "_NA_">${productStoreShipmentMethod.partyId?if_exists}</#if>&nbsp;${productStoreShipmentMethod.get("description",locale)?default("")}</option>
                          </#if>
                        </#list>
                      </select>
                    <#else/>
                      <#if shipGroup.carrierPartyId != "_NA_">
                        ${shipGroup.carrierPartyId?if_exists}
                      </#if>
                      ${shipGroup.shipmentMethodType.get("description",locale)?default("")}
                    </#if>
                  </div>
                </#if>
              </@infoRowNested>
            </#if>

            <@infoRowNested title=uiLabelMap.CrmOrderThirdPartyAccountNo>
              <@inputText name="thirdPartyAccountNumber" id="thirdPartyAccountNumber" size="10" maxlength="10" default="${shipGroup.thirdPartyAccountNumber?if_exists}" />
              ${uiLabelMap.CrmOrderThirdPartyAccountZip}: <@inputText name="thirdPartyPostalCode" id="thirdPartyPostalCode" size="10" maxlength="10" default="${shipGroup.thirdPartyPostalCode?if_exists}"/>
              <@inputText name="thirdPartyCountryGeoCode" id="thirdPartyCountryGeoCode" size="3" maxlength="3" default="${shipGroup.thirdPartyCountryGeoCode?if_exists}"/>
            </@infoRowNested>

            <#if !order.isCancelled() && !order.isCompleted() && !order.isRejected()>
              <@infoRowNested title="">
                <div class="tabletext">
                  <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                </div>
              </@infoRowNested>
            </#if>

            <#assign noShipment = (!shipGroup.contactMechId?has_content && !shipGroup.shipmentMethodTypeId?has_content) />
            <#if noShipment>
              <tr>
                <td colspan="3" align="center">
                  <div class="tableheadtext">${uiLabelMap.OrderNotShipped}</div>
                </td>
              </tr>
            </#if>
          </table>
        </form> <#-- end of updateOrderItemShipGroup form -->
      
        <table width="100%" border="0" cellpadding="1" cellspacing="0">
          <#-- drop ship order -->
          <#if shipGroup.supplier?has_content>
            <@infoSepBar/>
            <@infoRow title="${uiLabelMap.ProductDropShipment} - ${uiLabelMap.PartySupplier}" content="${shipGroup.supplier.description?default(shipGroup.supplierPartyId)}" />
          </#if>

          <#-- tracking number -->
          <#if shipGroup.trackingNumber?has_content || shipGroup.shipmentInfoSummaries?has_content>
            <@infoSepBar/>
            <@infoRowNested title=uiLabelMap.OrderTrackingNumber>
              <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
              <#if shipGroup.trackingNumber?has_content>
                <div class="tabletext">${shipGroup.trackingNumber}</div>
              </#if>
              <#if shipGroup.groupedShipmentInfoSummaries?has_content>
                <#list shipGroup.groupedShipmentInfoSummaries as shipmentInfoSummary>
                  <div class="tabletext">
                    <#if shipGroup.groupedShipmentInfoSummaries?size gt 1>${shipmentInfoSummary.shipmentPackageSeqId}: </#if>
                    ${uiLabelMap.CommonIdCode}: ${shipmentInfoSummary.trackingCode?default("[${uiLabelMap.OrderNotYetKnown}]")}
                    <#if shipmentInfoSummary.boxNumber?has_content> ${uiLabelMap.ProductBox} #${shipmentInfoSummary.boxNumber}</#if>
                    <#if shipmentInfoSummary.carrierPartyId?has_content>(${uiLabelMap.ProductCarrier}: ${shipmentInfoSummary.carrierPartyId})</#if>
                  </div>
                </#list>
              </#if>
            </@infoRowNested>
          </#if>

          <#if shipGroup.maySplit?has_content && !noShipment>
            <@infoSepBar/>
            <@infoRowNested title=uiLabelMap.OrderSplittingPreference>
              <div class="tabletext">
                <#if shipGroup.maySplit?upper_case == "N">
                  ${uiLabelMap.FacilityWaitEntireOrderReady}
                  <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && !order.isCancelled() && !order.isCompleted()>
                    <@submitFormLink form="allowSplitOrderAction" text=uiLabelMap.OrderAllowSplit />
                    <@form name="allowSplitOrderAction" url="allowordersplit" orderId=order.orderId shipGroupSeqId=shipGroup.shipGroupSeqId />
                  </#if>
                <#else/>
                  ${uiLabelMap.FacilityShipAvailable}
                </#if>
              </div>
            </@infoRowNested>
          </#if>

          <#if shipGroup.shippingInstructions?has_content>
            <@infoSepBar/>
            <@infoRow title=uiLabelMap.OpentapsInstructions content=shipGroup.shippingInstructions />
          </#if>

          <#if shipGroup.isGift?has_content && !noShipment>
            <@infoSepBar/>
            <@infoRowNested title="${uiLabelMap.OrderGift}?">
              <div class="tabletext">
                <#if shipGroup.isGift?upper_case == "N">${uiLabelMap.OrderThisOrderNotGift}<#else>${uiLabelMap.OrderThisOrderGift}</#if>
              </div>
            </@infoRowNested>
          </#if>

          <#if shipGroup.giftMessage?has_content>
            <@infoSepBar/>
            <@infoRow title=uiLabelMap.OrderGiftMessage content=shipGroup.giftMessage />
          </#if>

          <#if shipGroup.shipAfterDate?has_content>
            <@infoSepBar/>
            <@infoRow title=uiLabelMap.OrderShipAfterDate content=getLocalizedDate(shipGroup.shipAfterDate, "DATE") />
          </#if>

          <#if shipGroup.shipByDate?has_content>
            <@infoSepBar/>
            <@infoRow title=uiLabelMap.OrderShipBeforeDate content=getLocalizedDate(shipGroup.shipByDate, "DATE") />
          </#if>

          <#if shipGroup.estimatedShipDate?has_content>
            <@infoSepBar/>
            <@infoRow title=uiLabelMap.CrmOrderEstimatedShipDateAbbr content=getLocalizedDate(shipGroup.estimatedShipDate, "DATE") />
          </#if>

          <#if shipGroup.primaryShipments?has_content>
            <@infoSepBar/>
            <@infoRowNested title=uiLabelMap.FacilityShipments>
              <#list shipGroup.primaryShipments as shipment>
                <#if !shipment.isCancelled()>
                  <div class="tabletext">${uiLabelMap.OrderNbr}<a href="<@ofbizUrl>PackingSlip.pdf?shipmentId=${shipment.shipmentId}</@ofbizUrl>" class="linktext">${shipment.shipmentId}</a> (${(shipment.status.get("description",locale))?default(shipment.statusId?if_exists)})</div>
                </#if>
              </#list>
            </@infoRowNested>
          </#if>
        </table>
      </div>
    </#list>
  </@frameSection>
  <script type="text/javascript">
    switchToShipGroup();
  </script>
</#if> <#-- end of if order.shipGroups?has_content -->

</#if> <#-- end of if order?exists -->
