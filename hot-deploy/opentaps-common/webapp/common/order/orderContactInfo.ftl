<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
                  (<a href="<@ofbizUrl>/findOrders?supplierPartyId=${order.mainExternalParty.partyId}&amp;performFind=Y</@ofbizUrl>" class="linktext">${uiLabelMap.OrderOtherOrders}</a>)
                <#else>
                  (<a href="<@ofbizUrl>/findOrders?partyId=${order.mainExternalParty.partyId}</@ofbizUrl>" class="linktext">${uiLabelMap.OrderOtherOrders}</a>)
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
                  </#if>
                <#else/>
                  <div class="tabletext">${uiLabelMap.CrmAddressUnknown}</div>
                  <#if order.isApproved()>
                    <div class="errorMessage">${uiLabelMap.CrmOrderCannotShipUntilShippingAddressSet}</div>
                  </#if>
                </#if>
              <#elseif contactMech.contactMechTypeId == "EMAIL_ADDRESS">
                <div class="tabletext">
                  <@form name="writeOrderEmailFmt" url="writeOrderEmail" orderId=order.orderId sendTo=contactMech.infoString />
                  <@submitFormLink form="writeOrderEmailFmt" text=contactMech.infoString class="linktext"/>
                  <#if order.isSalesOrder() && security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>
                    <br/>(<a href="<@ofbizUrl>writeOrderConfirmationEmail?orderId=${order.orderId}&amp;partyId=${order.mainExternalParty.partyId}&amp;sendTo=${contactMech.infoString}</@ofbizUrl>" class="linktext">${uiLabelMap.OrderSendConfirmationEmail}</a>)
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
</#if> <#-- end of if order.mainExternalParty?exists -->

</#if> <#-- end of if order?exists -->
