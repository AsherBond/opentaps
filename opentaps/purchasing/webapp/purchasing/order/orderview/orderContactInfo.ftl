<#--
 * Copyright (c) Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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

<#if order.mainExternalParty?has_content || orderContactMechValueMaps?has_content>
  <div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderContactInformation}</div>
    </div>
    <div class="screenlet-body">
      <table width="100%" border="0" cellpadding="1" cellspacing="0">

        <@infoRowNested title=uiLabelMap.CommonName>
          <div class="tabletext">
            <#if order.mainExternalParty?has_content>
              <span>${order.mainExternalParty.createViewPageLink(externalLoginKey)}</span>
              <span>
                <#if order.isPurchaseOrder()>
                  (<a href="<@ofbizUrl>/findOrders?performFind=Y&partyIdSearch=${order.mainExternalParty.partyId}&amp;performFind=Y</@ofbizUrl>" class="linktext">${uiLabelMap.OrderOtherOrders}</a>)
                <#else>
                  (<a href="<@ofbizUrl>/findOrders?performFind=Y&partyIdSearch=${order.mainExternalParty.partyId}</@ofbizUrl>" class="linktext">${uiLabelMap.OrderOtherOrders}</a>)
                </#if>
              </span>
            </#if>
          </div>
          <#if order.mainExternalParty.hasClassification("DONOTSHIP_CUSTOMERS")>
            <p class="requiredField">${uiLabelMap.OpentapsDoNotShipCustomer}</p>
          </#if>
        </@infoRowNested>

        <#-- List the contact mech associated to the order that are not phone numbers (they are displayed in a special section) -->
        <#list order.orderContactMeches as orderContactMech>
          <#assign contactMech = orderContactMech.contactMech/>
          <#-- Telecom numbers will be shown in separate loop -->
          <#if contactMech.contactMechTypeId != "TELECOM_NUMBER">
            <#assign contactMechPurpose = orderContactMech.contactMechPurposeType/>
            <@infoSepBar/>
            <@infoRowNested title=contactMechPurpose.get("description",locale)>
              <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
                <#if contactMech.contactMechId != "_NA_">
                  <#if contactMech.postalAddress?has_content>
                    <div class="tabletext">
                      <#if contactMech.postalAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${contactMech.postalAddress.toName}<br/></#if>
                      <#if contactMech.postalAddress.attnName?has_content><b>${uiLabelMap.CommonAttn}:</b> ${contactMech.postalAddress.attnName}<br/></#if>
                      ${contactMech.postalAddress.address1?if_exists}<br/>
                      <#if contactMech.postalAddress.address2?has_content>${contactMech.postalAddress.address2}<br/></#if>
                      ${contactMech.postalAddress.city?if_exists}<#if contactMech.postalAddress.stateProvinceGeoId?has_content>, ${contactMech.postalAddress.stateProvinceGeoId} </#if>
                      ${contactMech.postalAddress.postalCode?if_exists}<br/>
                      <#if contactMech.postalAddress.countryGeoId?has_content ><@displayGeo geoId=contactMech.postalAddress.countryGeoId /><br/></#if>
                    </div>
                    <#if "SHIPPING_LOCATION" == contactMechPurpose.get("contactMechPurposeTypeId") >
                      <#assign shippingContactMechId = contactMech.contactMechId />
                    </#if>
                  </#if>
                <#else/>
                  <div class="tabletext">${uiLabelMap.PurchNoShippingAddress}</div>
                  <#assign shippingContactMechId = contactMech.contactMechId />
                </#if>
              <#elseif contactMech.contactMechTypeId == "EMAIL_ADDRESS">
                <div class="tabletext">
                  <a href="<@ofbizContentUrl>writeEmail?orderId=${order.orderId}&toEmail=${contactMech.infoString}</@ofbizContentUrl>" class="linktext">${contactMech.infoString}</a>
                  <#if security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>
                     <form name="writeOrderEmailForm" method="post" action="<@ofbizUrl>writeOrderEmail</@ofbizUrl>"/>
                         <@inputHidden name="orderId" value="${order.orderId}"/>
                         <@inputHidden name="partyId" value="${order.mainExternalParty.partyId}"/>
                         <@inputHidden name="sendTo" value="${contactMech.infoString}"/>
                     </form>
                     (<@submitFormLink form="writeOrderEmailForm" text=uiLabelMap.OrderSendConfirmationEmail class="linktext"/></a>)
                  </#if>
                </div>
              <#elseif contactMech.contactMechTypeId == "WEB_ADDRESS">
                <div class="tabletext">
                  ${contactMech.infoString}
                  <#assign openString = contactMech.infoString/>
                  <#if !openString?starts_with("http") && !openString?starts_with("HTTP")>
                    <#assign openString = "http://" + openString/>
                  </#if>
                  <a target="_blank" href="${openString}" class="buttontext">(open&nbsp;page&nbsp;in&nbsp;new&nbsp;window)</a>
                </div>
              <#else/>
                <div class="tabletext">
                  ${contactMech.infoString?if_exists}
                </div>
              </#if>
            </@infoRowNested>
          </#if>
        </#list>
      </table>
      <#-- Telecom numbers are shown always in the end of list and hidden by default. -->
      <#if order.getOrderAndMainExternalPartyPhoneNumbers()?has_content>
      <table width="100%" border="0" cellpadding="1" cellspacing="0">
          <tr><td colspan="7"></td></tr>
      </table>
      <@flexArea targetId="TelecomNumbersPane" title=uiLabelMap.OpentapsPhoneNumbers controlStyle="font-weight: bold;">
          <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr><td colspan="7">&nbsp;</td></tr>
              <#list order.getOrderAndMainExternalPartyPhoneNumbers() as telecomNumber>
                <#list order.getContactMechPurposeTypesForContactMech(telecomNumber.contactMech) as purposeType>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${purposeType.get("description",locale)}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">
                        ${telecomNumber.countryCode?if_exists}
                        <#if telecomNumber.areaCode?exists>${telecomNumber.areaCode}-</#if>${telecomNumber.contactNumber}
                        <#if telecomNumber.extension?exists>ext&nbsp;${telecomNumber.extension}</#if>
                      </div>
                    </td>
                  </tr>
                  <#if telecomNumber_has_next || purposeType_has_next>
                    <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                  <#else>
                    <tr><td colspan="7">&nbsp;</td></tr>
                  </#if>
                </#list>
              </#list>
          </table>
      </@flexArea>
      <table width="100%" border="0" cellpadding="1" cellspacing="0">
        <tr><td colspan="7">&nbsp;</td></tr>
      </table>
    </#if>
  </div>
</div>

<script type="text/javascript">
  /*<![CDATA[*/

    function editCurrentAddress(/*String*/ shipGroupSeqId, /*String*/ oldContactMechId) {
      var url = "<@ofbizUrl>editPurchaseOrderContactMech?partyId=${order.mainExternalParty.partyId}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=orderview%3ForderId%3D${order.orderId}&orderId=${order.orderId}</@ofbizUrl>";
      // get selected contact mech
      var contactMechIdInput = document.getElementById('newOrderContactMechId');
      var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
      if ("" == contactMechId || "_NA_" == contactMechId) return;
      url += "&contactMechId=" + contactMechId + "&shipGroupSeqId=" + shipGroupSeqId + "&oldContactMechId=" + oldContactMechId;
      location.href = url;
    }

    function newContactMechIfSelected(/*String*/ shipGroupSeqId, /*String*/ oldContactMechId) {
      var contactMechIdInput = document.getElementById('newOrderContactMechId');
      var contactMechId = contactMechIdInput.options[contactMechIdInput.selectedIndex].value;
      if ("_NEW_" == contactMechId) {
        var url = "<@ofbizUrl>editPurchaseOrderContactMech?preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=orderview%3ForderId%3D${order.orderId}&orderId=${order.orderId}</@ofbizUrl>";
        url += "&shipGroupSeqId=" + shipGroupSeqId + "&oldContactMechId=" + oldContactMechId;
        location.href = url;
      }
    }

  /*]]>*/
</script>

<#assign shipGroupSeqId = ((order.shipGroups?first).shipGroupSeqId)?if_exists />

<div class="screenlet">
  <div class="screenlet-header">
      <div class="boxhead">&nbsp;${uiLabelMap.PurchShippingInformation}</div>
  </div>
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <form method="post" action="setPurchOrderShippingAddress" name="changeShippingAddressForm">
        <@inputHidden name="orderId" value=orderId />
      <tr>
        <td align="right" width="15%">
          <@display text="${uiLabelMap.OrderAddress}" class="tableheadtext" />
        </td>
        <td width="5">&nbsp;</td>
        <td width="80%">
          <select name="newOrderContactMechId" id="newOrderContactMechId" class="selectBox" onChange="newContactMechIfSelected('${shipGroupSeqId!}', '${shippingContactMechId!}')">
            <#list facilityMaps as facilityMap>
              <#assign facility = facilityMap.facility?if_exists>
              <#assign facilityContactMechList = facilityMap.facilityContactMechList>
              <#if facilityContactMechList?has_content>
                <#list facilityContactMechList as facilityContactMech>
                  <#if facilityContactMech.postalAddress?exists>
                    <#assign address = facilityContactMech.postalAddress>
                    <option value="${address.contactMechId}"  <#if address.contactMechId==shippingContactMechId?default("_NA_")> selected="selected"</#if>>
                      <#if facility?has_content>${facility.facilityName?if_exists}<#else>${address.toName?if_exists}</#if> ${uiLabelMap.CommonAt} ${address.address1} - ${address.city?if_exists} ${address.countryGeoId?if_exists}
                    </option>
                  </#if>
                </#list> 
              </#if>          
            </#list>
            <option value="_NA_" <#if shippingContactMechId?if_exists=="_NA_"> selected="selected"</#if>>${uiLabelMap.PurchNoShippingAddress}</option>
            <option disabled="disabled">--------</option>
            <option value="_NEW_">${uiLabelMap.CommonNew}</option>
          </select>
          <a id="editContactMechButton" class="buttontext" href="#" onClick="editCurrentAddress('${shipGroupSeqId!}', '${shippingContactMechId!}');">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
      <tr>
        <td></td><td width="5">&nbsp;</td>
        <td width="80%">
          <@inputSubmit title=uiLabelMap.CommonUpdate />
        </td>
      </tr>
        </form>
    </table>
  </div>
</div>

</#if> <#-- end of if order.mainExternalParty?exists -->

</#if> <#-- end of if order?exists -->
