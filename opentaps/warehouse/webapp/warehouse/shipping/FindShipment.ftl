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
/*<![CDATA[*/
function lookupShipments() {
    shipmentIdValue = document.lookupShipmentForm.shipmentId.value;
    if (shipmentIdValue.length > 1) {
        document.lookupShipmentForm.action = "<@ofbizUrl>ViewShipment</@ofbizUrl>";
    } else {
        document.lookupShipmentForm.action = "<@ofbizUrl>${parameters.thisRequestUri}</@ofbizUrl>";
    }
    document.lookupShipmentForm.submit();
}
/*]]>*/
</script>

<form method="post" name="lookupShipmentForm" action="<@ofbizUrl>${parameters.thisRequestUri}</@ofbizUrl>">
  <@inputHidden name="lookupFlag" value="Y"/>
  <@inputHidden name="formShipmentTypeId" value="${parameters.formShipmentTypeId?if_exists}"/>

  <div class="screenlet">
    <@sectionHeader title=uiLabelMap.ProductFindShipment headerClass="screenlet-header" titleClass="boxhead"/>
    <table class="twoColumnForm" style="border:0">
      <@inputTextRow name="shipmentId" title="${uiLabelMap.ProductShipmentId} :"/>
      <@inputSelectRow name="shipmentTypeId" title="${uiLabelMap.ProductShipmentType} :" list=shipmentTypes displayField="description" default=(currentShipmentType.shipmentTypeId)?if_exists required=false defaultOptionText=uiLabelMap.ProductAnyShipmentType />
      <#if isIncomingShipment>
        <@inputLookupRow name="partyIdFrom" title="${uiLabelMap.ProductFromParty} :" lookup="LookupPartyName" form="lookupShipmentForm" default=(currentPartyIdFrom.partyId)?if_exists />
      <#else>
        <@inputLookupRow name="partyIdTo" title="${uiLabelMap.ProductToParty} :" lookup="LookupPartyName" form="lookupShipmentForm" default=(currentPartyIdTo.partyId)?if_exists />
      </#if>
      <@inputSelectRow name="statusId" title="${uiLabelMap.ProductStatus} :" list=shipmentStatusList displayField="description" default=(currentStatus.statusId)?if_exists required=false />
      <@inputLookupRow name="lotId" title="${uiLabelMap.ProductLotId} :" lookup="LookupLot" form="lookupShipmentForm" default=(currentLot.lotId)?if_exists />
      <@inputSubmitRow title=uiLabelMap.ProductLookupShipment />
    </table>
  </div>
</form> 

<div class="screenlet">
  <@paginate name="listShipments" list=shipmentListBuilder rememberPage=false isIncomingShipment=isIncomingShipment>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable" style="border:0">
      <tr class="listTableHeader">
        <#if parameters.isIncomingShipment>
          <@headerCell width="35%" title=uiLabelMap.ProductShipmentId orderBy="shipmentId"/>
          <@headerCell width="50%" title=uiLabelMap.ProductFromParty orderBy="fromPartyId"/>
          <@headerCell width="15%" title=uiLabelMap.ProductStatus orderBy="statusId"/>
        <#else>
          <@headerCell width="20%" title=uiLabelMap.ProductShipmentId orderBy="shipmentId"/>
          <@headerCell width="30%" title=uiLabelMap.ProductToParty orderBy="toPartyId"/>
          <@headerCell width="35%" title=uiLabelMap.ProductShipToAddress orderBy="destinationContactMechId"/>
          <@headerCell width="15%" title=uiLabelMap.ProductStatus orderBy="statusId"/>
        </#if>
      </tr>
      <#list pageRows as shipment>
        <tr class="${tableRowClass(shipment_index)}">
          <td>
            <span class="tabletext">${shipment.shipmentTypeDescription?default(shipment.shipmentTypeId?default(""))}</span>
            #<a href="<@ofbizUrl>ViewShipment?shipmentId=${shipment.shipmentId}</@ofbizUrl>" class="buttontext">${shipment.shipmentId}</a>
          </td>
          <#if parameters.isIncomingShipment>
            <@displayCell text="${shipment.fromParty!}" />
          <#else>
            <@displayCell text="${shipment.toParty!}" />
            <@displayCell text="${shipment.destinationAddress!}" />
          </#if>
          <@displayCell text="${shipment.statusDescription?default(shipment.statusId?default('N/A'))}"/>
        </tr>
      </#list>          
    </table>
  </#noparse>
  </@paginate>
</div>
