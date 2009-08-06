<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<#-- This file has been modified from the version included with the Apache licensed OFBiz product application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">
<!-- //
function lookupShipments() {
    shipmentIdValue = document.lookupShipmentForm.shipmentId.value;
    if (shipmentIdValue.length > 1) {
        document.lookupShipmentForm.action = "<@ofbizUrl>ViewShipment</@ofbizUrl>";
    } else {
        document.lookupShipmentForm.action = "<@ofbizUrl>${parameters.thisRequestUri}</@ofbizUrl>";
    }
    document.lookupShipmentForm.submit();
}
// -->
</script>

<form method="post" name="lookupShipmentForm" action="<@ofbizUrl>${parameters.thisRequestUri}</@ofbizUrl>">
<input type="hidden" name="lookupFlag" value="Y">
<@inputHidden name="formShipmentTypeId" value="${parameters.formShipmentTypeId?if_exists}"/>
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td><div class="boxhead">${uiLabelMap.ProductFindShipment}</div></td>
          <td align="right">
            <div class="tabletext">
              <#if requestParameters.facilityId?has_content>
              <a href="<@ofbizUrl>quickShipOrder?facilityId=${requestParameters.facilityId}</@ofbizUrl>" class="submenutext">${uiLabelMap.ProductQuickShipOrder}</a></#if>
            </div>
          </td>
        </tr>
      </table>
      <table width="100%" border="0" cellspacing="0" cellpadding="2" class="boxbottom">
        <tr>
          <td align="center" width="100%">
            <table class="twoColumnForm">
              <@inputTextRow name="shipmentId" title="${uiLabelMap.ProductShipmentId} :"/>
              <@inputSelectRow name="shipmentTypeId" title="${uiLabelMap.ProductShipmentType} :" list=shipmentTypes displayField="description" default=(currentShipmentType.shipmentTypeId)?if_exists required=false defaultOptionText=uiLabelMap.ProductAnyShipmentType />
              <#if parameters.formShipmentTypeId?if_exists == "INCOMING_SHIPMENT">
                <@inputLookupRow name="partyIdFrom" title="${uiLabelMap.ProductFromParty} :" lookup="LookupPartyName" form="lookupShipmentForm" default=(currentPartyIdFrom.partyId)?if_exists />
              <#else>
                <@inputLookupRow name="partyIdTo" title="${uiLabelMap.ProductToParty} :" lookup="LookupPartyName" form="lookupShipmentForm" default=(currentPartyIdTo.partyId)?if_exists />
              </#if>
              <@inputSelectRow name="statusId" title="${uiLabelMap.ProductStatus} :" list=shipmentStatusList displayField="description" default=(currentStatus.statusId)?if_exists required=false />
              <@inputLookupRow name="lotId" title="${uiLabelMap.ProductLotId} :" lookup="LookupLot" form="lookupShipmentForm" default=(currentLot.lotId)?if_exists />
              <@inputSubmitRow title=uiLabelMap.ProductLookupShipment />
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</form> 


<#if shipmentList?exists>
<br/>
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td width="50%"><div class="boxhead">${uiLabelMap.ProductShipmentsFound}</div></td>
          <td width="50%">
            <div class="boxhead" align="right">
              <#if 0 < shipmentList?size>             
                <#if (viewIndex > 1)>
                  <a href="<@ofbizUrl>${parameters.thisRequestUri}?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}${paramList}&lookupFlag=Y</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonPrevious}</a>
                <#else>
                  <span class="submenutextdisabled">${uiLabelMap.CommonPrevious}</span>
                </#if>
                <#if (listSize > 0)>
                  <span class="submenutextinfo">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
                </#if>
                <#if (listSize > highIndex)>
                  <a href="<@ofbizUrl>${parameters.thisRequestUri}?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}${paramList}&lookupFlag=Y</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonNext}</a>
                <#else>
                  <span class="submenutextrightdisabled">${uiLabelMap.CommonNext}</span>
                </#if>
              </#if>
              &nbsp;
            </div>
          </td>
        </tr>
      </table>
      <table width="100%" border="0" cellspacing="0" cellpadding="2" class="boxbottom">
        <tr>
          <#if parameters.formShipmentTypeId?if_exists == "INCOMING_SHIPMENT">
            <td width="35%" align="left"><div class="tableheadtext">${uiLabelMap.ProductShipmentId}</div></td>
            <td width="50%" align="left"><div class="tableheadtext">${uiLabelMap.ProductFromParty}</div></td>
            <td width="15%" align="left"><div class="tableheadtext">${uiLabelMap.ProductStatus}</div></td>
          <#else>
            <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.ProductShipmentId}</div></td>
            <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.ProductToParty}</div></td>
            <td width="35%" align="left"><div class="tableheadtext">${uiLabelMap.ProductShipToAddress}</div></td>
            <td width="15%" align="left"><div class="tableheadtext">${uiLabelMap.ProductStatus}</div></td>
          </#if>
        </tr>
        <tr>
          <td colspan="10"><hr class="sepbar"></td>
        </tr>
        <#if shipmentList?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list shipmentList as shipment>
            <#-- TODO: Move all this processing to the BSH file -->
            <#assign statusItem = shipment.getRelatedOneCache("StatusItem")>
            <#assign shipmentType = shipment.getRelatedOneCache("ShipmentType")?if_exists>
            <#assign fromPartyId = shipment.partyIdFrom?if_exists>
            <#assign toPartyId = shipment.partyIdTo?if_exists>
            <#if fromPartyId?has_content>
              <#assign fromPartyName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, fromPartyId, false) />
            </#if>
            <#if toPartyId?has_content>
              <#assign toPartyName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, toPartyId, false) />
            </#if>
            <#assign destinationContactMechId = shipment.destinationContactMechId?if_exists>
            <#if destinationContactMechId?has_content>
              <#assign destinationPostalAddress = delegator.findByPrimaryKeyCache("PostalAddress", Static["org.ofbiz.base.util.UtilMisc"].toMap("contactMechId", destinationContactMechId))?if_exists>
            </#if>
            <tr class="${rowClass}">
              <td>
                <span class="tabletext">${shipmentType.get("description",locale)?default(shipmentType.shipmentTypeId?default(""))}</span>
                #<a href="<@ofbizUrl>ViewShipment?shipmentId=${shipment.shipmentId}</@ofbizUrl>" class="buttontext">${shipment.shipmentId}</a>
              </td>
              <#if parameters.formShipmentTypeId?if_exists == "INCOMING_SHIPMENT">
                <td><div class="tabletext"><#if fromPartyId?has_content>${fromPartyName} (${fromPartyId})</#if></div></td>
              <#else>
                <td><div class="tabletext"><#if toPartyId?has_content>${toPartyName} (${toPartyId})</#if></div></td>
                <td><div class="tabletext">
                  <#if destinationPostalAddress?exists>${destinationPostalAddress.address1?if_exists} - ${destinationPostalAddress.city?if_exists}</#if>
                </div></td>
              </#if>
              <td><div class="tabletext">${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</div></td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "viewManyTR2">
              <#assign rowClass = "viewManyTR1">
            <#else>
              <#assign rowClass = "viewManyTR2">
            </#if>
          </#list>          
        <#else>
          <tr>
            <td colspan="4"><div class="head3">${uiLabelMap.ProductNoShipmentsFound}.</div></td>
          </tr>        
        </#if>
      </table>
    </td>
  </tr>
</table>
</#if>
