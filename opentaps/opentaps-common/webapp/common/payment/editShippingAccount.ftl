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
    <#if !partyCarrierAccount?exists>
      <form method="post" action="<@ofbizUrl>createPartyCarrierAccount?DONE_PAGE=${donePage}</@ofbizUrl>" name="editshippingaccount" style="margin: 0;">
    <#else>
      <form method="post" action="<@ofbizUrl>updatePartyCarrierAccount?DONE_PAGE=${donePage}</@ofbizUrl>" name="editshippingaccount" style="margin: 0;">
    </#if>
  <input type="hidden" name="partyId" value="${partyId}"/>
  <table class="basic-table" cellspacing="0">
      <tr>
          <@displayCell text=uiLabelMap.OpentapsCarrier blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>
          <#if !partyCarrierAccount?exists>
              <@inputSelectCell list=carrierParties name="carrierPartyId" key="partyId" displayField="groupName" required=true default=partyCarrierAccount?if_exists.carrierPartyId?if_exists  />
          <#else>
              <@inputHidden name="carrierPartyId" value=partyCarrierAccount?if_exists.carrierPartyId?if_exists />
              <#assign name = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, partyCarrierAccount?if_exists.carrierPartyId?if_exists, false)>
              <@displayCell blockClass="groupName" text="${name} (${partyCarrierAccount?if_exists.carrierPartyId?if_exists})"  />
          </#if>
      </tr>
      <tr>
          <@displayCell text=uiLabelMap.CommonFromDateTime blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>          
          <#if !partyCarrierAccount?exists>
              <@inputDateTimeCell name="fromDate" form="editshippingaccount" default=partyCarrierAccount?if_exists.fromDate?if_exists/>
          <#else>
              <@inputHidden name="fromDate" value=partyCarrierAccount?if_exists.fromDate?if_exists />
              <@displayDateCell date=partyCarrierAccount?if_exists.fromDate?if_exists/>
          </#if>
      </tr>
      <tr>
          <@displayCell text=uiLabelMap.CommonThruDateTime blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>          
          <@inputDateTimeCell name="thruDate" form="editshippingaccount" default=partyCarrierAccount?if_exists.thruDate?if_exists/>
      </tr>      
      <tr>
          <@displayCell text=uiLabelMap.OpentapsAccountNumber blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>
          <@inputTextCell name="accountNumber" default=partyCarrierAccount?default({}).accountNumber?if_exists/>
      </tr>
      <tr>
          <@displayCell text=uiLabelMap.OpentapsPostalCode blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>
          <@inputTextCell name="postalCode" default=partyCarrierAccount?default({}).postalCode?if_exists size="10"/>
      </tr>
      <tr>
          <@displayCell text=uiLabelMap.OpentapsCountry blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>
          <td>
          <#if partyCarrierAccount?exists && partyCarrierAccount.countryGeoCode?has_content>
              <#assign address = {"countryGeoId":partyCarrierAccount.countryGeoCode}>
          </#if>
          <@inputCountry name="countryGeoCode" address=address />
          </td>
      </tr>
      <tr>
          <@displayCell text=uiLabelMap.OpentapsMakeDefault blockClass="titleCell" blockStyle="width: 20px" class="tableheadtext"/>
          <@inputIndicatorCell name="isDefault" required=true default=partyCarrierAccount?default({}).isDefault?default("N")/>
      </tr>
    <tr><td colspan="2">&nbsp;</td></tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <a href="javascript:document.editshippingaccount.submit()" class="subMenuButton">${uiLabelMap.CommonSave}</a>
        <@inputConfirm title=uiLabelMap.CommonCancel href="${donePage}?partyId=${partyId}"/>
      </td>
    </tr>
  </table>
  </form>



