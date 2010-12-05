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

<#if order.mainExternalParty?has_content || orderContactMechValueMaps?has_content>

  <@frameSection title=uiLabelMap.OrderContactInformation>
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
        <#if !orderContactMechs?exists><#assign orderContactMechs = order.orderContactMeches /></#if> <#-- we now order the contact mechs by type, fallback to the previous method for compatibility -->
        <#assign hasOrderEmail = false/>
        <#assign hasBillingAddress = false/>
        <#list orderContactMechs as orderContactMech>
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
                  </#if>
                  <#if orderContactMech.contactMechPurposeTypeId == "BILLING_LOCATION">
                    <#assign hasBillingAddress = true />
                    <#if mainPartyBillingAddresses?has_content>
                      <#assign altBillingAddresses = mainPartyBillingAddresses.clone() />
                      <#assign dummy = altBillingAddresses.add(contactMech.postalAddress) />
                      <div class="tabletext">
                        <@form name="updateOrderBillingContact" url="updateOrderContactMech" orderId=order.orderId contactMechPurposeTypeId=orderContactMech.contactMechPurposeTypeId contactMechTypeId=contactMech.contactMechTypeId oldContactMechId=contactMech.contactMechId >
                          <@inputSelect name="contactMechId" list=altBillingAddresses default=contactMech.contactMechId key="contactMechId" ; address>
                            ${address.address1!}
                            <#if address.city?has_content> - ${address.city!}</#if>
                            <#if address.stateProvinceGeoId?has_content> - ${address.stateProvinceGeoId!} </#if>
                          </@inputSelect>
                          <@inputSubmit title=uiLabelMap.CommonUpdate />
                        </@form>
                      </div>
                    </#if>
                  </#if>
                <#else/>
                  <div class="tabletext">${uiLabelMap.CrmAddressUnknown}</div>
                  <#if order.isApproved()>
                    <div class="errorMessage">${uiLabelMap.CrmOrderCannotShipUntilShippingAddressSet}</div>
                  </#if>
                </#if>
                
              <#elseif contactMech.contactMechTypeId == "EMAIL_ADDRESS">
                <#assign hasOrderEmail = true/>
                <div class="tabletext">
                  <@form name="writeOrderEmailAction" url="writeOrderEmail" orderId=order.orderId sendTo=contactMech.infoString />
                  <@form name="writeOrderConfirmationEmailAction" url="writeOrderConfirmationEmail" donePage="orderview" orderId=order.orderId sendTo=contactMech.infoString partyId=order.mainExternalParty.partyId/>
                  <@submitFormLink form="writeOrderEmailAction" text=contactMech.infoString class="linktext"/>
                  <#if order.isSalesOrder() && security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>
                    <br/>(<@submitFormLink form="writeOrderConfirmationEmailAction" class="linktext" text=uiLabelMap.OrderSendConfirmationEmail />)
                  </#if>
                </div>
                <#if mainPartyEmailAddresses?has_content>
                  <#assign altEmailAddresses = mainPartyEmailAddresses.clone() />
                  <#assign dummy = altEmailAddresses.add(contactMech) />
                  <div class="tabletext">
                    <@form name="updateOrderEmailContact" url="updateOrderContactMech" orderId=order.orderId contactMechPurposeTypeId=orderContactMech.contactMechPurposeTypeId contactMechTypeId=contactMech.contactMechTypeId oldContactMechId=contactMech.contactMechId >
                      <@inputSelect name="contactMechId" list=altEmailAddresses default=contactMech.contactMechId key="contactMechId" displayField="infoString" />
                      <@inputSubmit title=uiLabelMap.CommonUpdate />
                    </@form>
                  </div>
                </#if>
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

        <#if !hasBillingAddress && mainPartyBillingAddresses?has_content>
          <@infoSepBar/>
          <@infoRowNested title=uiLabelMap.OrderBillingAddress>
            <#assign altBillingAddresses = mainPartyBillingAddresses.clone() />
            <@form name="createOrderBillingContact" url="createOrderContactMech" orderId=order.orderId contactMechPurposeTypeId="BILLING_LOCATION" contactMechTypeId="POSTAL_ADDRESS" oldContactMechId="" >
              <@inputSelect name="contactMechId" list=altBillingAddresses key="contactMechId" required=false ; address>
                ${address.address1!}
                <#if address.city?has_content> - ${address.city!}</#if>
                <#if address.stateProvinceGeoId?has_content> - ${address.stateProvinceGeoId!} </#if>
              </@inputSelect>
              <@inputSubmit title=uiLabelMap.CommonUpdate />
            </@form>
          </@infoRowNested>
        </#if>

        <#if !hasOrderEmail>
          <@infoSepBar/>
          <@infoRowNested title=uiLabelMap.CommonEmail>
            <#if mainPartyEmailAddresses?has_content>
              <@form name="updateOrderEmailContact" url="createOrderContactMech" orderId=order.orderId contactMechPurposeTypeId="ORDER_EMAIL" contactMechTypeId="EMAIL_ADDRESS">
                <@inputSelect name="contactMechId" list=mainPartyEmailAddresses key="contactMechId" displayField="infoString" />
                <@inputSubmit title=uiLabelMap.CommonAdd />
              </@form>
            <#else>
              ${uiLabelMap.CommonNoContactInformationOnFile}
            </#if>
          </@infoRowNested>
        </#if>
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
  </@frameSection>
</#if> <#-- end of if order.mainExternalParty?exists -->

</#if> <#-- end of if order?exists -->
