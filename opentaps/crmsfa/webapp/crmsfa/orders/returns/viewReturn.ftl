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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#if returnHeader?has_content>
<#if returnHeader.statusId != "RETURN_CANCELLED">
<#if canCancelReturn?exists>
    <@form name="cancelReturnAction" url="updateReturnHeader" returnId=returnId statusId="RETURN_CANCELLED"/>
    <#assign cancelLink><@submitFormLinkConfirm form="cancelReturnAction" text=uiLabelMap.CommonCancel class="subMenuButtonDangerous"/></#assign>
</#if>
<#if canAcceptReturn?exists && returnItems.size() != 0>
    <@form name="acceptReturnAction" url="acceptReturn" returnId=returnId shipmentId="${returnShipmentId!}"/>
    <#assign acceptLink><@submitFormLink form="acceptReturnAction" text=uiLabelMap.OpentapsAccept class="subMenuButton"/></#assign>
</#if>
<#if returnItems.size() != 0>
    <#assign pdfLink><a href="return.pdf?returnId=${returnId}" class="subMenuButton">PDF</a></#assign>
</#if>
<#if canForceCompleteReturn?exists>
    <@form name="forceCompleteReturnAction" url="updateReturnHeader" returnId=returnId statusId="RETURN_COMPLETED"/>
    <#assign forceCompleteLink><@submitFormLinkConfirm form="forceCompleteReturnAction" text=uiLabelMap.OpentapsForceComplete class="subMenuButtonDangerous"/></#assign>
</#if>
<#if returnHeader.statusId == "RETURN_REQUESTED" && returnHeader.destinationFacilityId?has_content && returnHeader.carrierReturnServiceId?has_content && returnHeader.originContactMechId?has_content && returnHeader.originPhoneContactMechId?has_content && returnHeader.estimatedWeight?has_content && returnItems.size() != 0 && ! returnShipment?exists>
    <@form name="createAndConfirmReturnShipmentAction" url="createAndConfirmReturnShipment" returnId=returnId/>
    <#assign confirmLink><@submitFormLink form="createAndConfirmReturnShipmentAction" text=uiLabelMap.CrmReturnSchedulePickup class="subMenuButton"/></#assign>
</#if>
</#if>
<div class="subSectionBlock">

<@frameSectionHeader title=uiLabelMap.CommonReturn extra="${pdfLink?if_exists}${confirmLink?if_exists}${acceptLink?if_exists}${forceCompleteLink?if_exists}${cancelLink?if_exists}"/>
<form action="<@ofbizUrl>updateReturnHeader</@ofbizUrl>" name="updateReturnHeader" method="post">
    <table class="twoColumnForm">
        <@inputHidden name="returnId" value=returnId />
        <@displayRow title=uiLabelMap.OrderReturnId text=returnId />
        <@displayRow title=uiLabelMap.CommonStatus text=returnStatus.description />
        <#if returnHeader.primaryOrderId?has_content>
        <@displayLinkRow title=uiLabelMap.CommonOrder href="orderview?orderId=${returnHeader.primaryOrderId}" text=returnHeader.primaryOrderId />
        </#if>
        <@displayRow title=uiLabelMap.CrmCustomer text=(customerName + " (${returnHeader.fromPartyId})") />
        <#assign defaultWeightStr = ("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval/>
        <#if returnHeader.statusId == "RETURN_REQUESTED">
          <@inputSelectRow name="originContactMechId" title=uiLabelMap.CrmReturnedFrom list=addresses key="contactMechId" default=returnHeader.originContactMechId required=false ; address>
            ${address.address1}, ${address.city?if_exists}, ${address.stateName?if_exists} ${address.postalCode?if_exists} ${address.countryName?if_exists}
          </@inputSelectRow>
          <@inputSelectRow name="destinationFacilityId" title=uiLabelMap.CrmReturnedTo list=warehouses key="facilityId" displayField="facilityName" default=returnHeader.destinationFacilityId required=false />
          <@inputSelectRow name="billingAccountId" title=uiLabelMap.CrmStoreCreditAccount list=billingAccounts key="billingAccountId" default=returnHeader.billingAccountId required=false ; account>
            ${account.description?if_exists} <#if account.billingAccountId?default("") != "NEW_ACCOUNT">(${account.billingAccountId?if_exists})</#if>
          </@inputSelectRow>
          <tr>
            <td class="tableheadtext titleCellTop">${uiLabelMap.CrmReturnService}</td>
            <td class="tabletext">
              <#if returnShipment?exists && returnService?exists>
                <@display text=returnService.description />
              <#else>
                <select class="inputBox" name="carrierReturnServiceId">
                  <option value="" ${(returnHeader.carrierReturnServiceId?default("") == "")?string("selected=\"selected\"", "")}>${uiLabelMap.CrmReturnCustomerWillReturn}</option>
                  <#list returnServices?sort_by("description") as returnService>
                    <option value="${returnService.carrierReturnServiceId}" ${(returnHeader.carrierReturnServiceId?default("") == returnService.carrierReturnServiceId)?string("selected=\"selected\"", "")}>${returnService.description}</option>
                  </#list>
                </select>
              </#if>
            </td> 
          </tr>
          <tr>
            <td class="tableheadtext titleCellTop">${uiLabelMap.CrmPickupPhoneNumber}</td>
            <td class="tabletext">
              <#if returnShipment?exists && originTelecomNumber?has_content>
                <#assign phoneNumberText><#if originTelecomNumber.countryCode?has_content>${originTelecomNumber.countryCode}-</#if>${originTelecomNumber.areaCode?if_exists}-${originTelecomNumber.contactNumber?if_exists}</#assign>
                <@display text=phoneNumberText />
              <#else>
                <select class="inputBox" name="originPhoneContactMechId">
                  <option value="" ${(returnHeader.originPhoneContactMechId?default("") == "")?string("selected=\"selected\"", "")}></option>
                  <#list phoneNumbers as phoneNumber>
                    <#assign phoneNumberText><#if phoneNumber.countryCode?has_content>${phoneNumber.countryCode}-</#if>${phoneNumber.areaCode?if_exists}-${phoneNumber.contactNumber?if_exists}</#assign>
                    <option value="${phoneNumber.contactMechId}" ${(returnHeader.originPhoneContactMechId?default("") == phoneNumber.contactMechId)?string("selected=\"selected\"", "")}>${phoneNumberText}</option>
                  </#list>
                </select>
              </#if>
            </td> 
            <#if returnShipment?exists>
              <@displayLinkRow title=uiLabelMap.CrmReturnShipment text=returnShipment.shipmentId href="/warehouse/control/ViewShipment?shipmentId=${returnShipment.shipmentId}"/>
              <#if returnShipmentRouteSegment?exists>
                <@displayRow title=uiLabelMap.CrmReturnCarrierShipmentMethod text="${carrierName?if_exists} ${shipmentMethodDescription?if_exists}"/>
                <@displayRow title=uiLabelMap.CrmReturnShipmentTrackingCode text=returnShipmentRouteSegment.trackingIdNumber?if_exists/>
              </#if>
              <@displayRow title="${uiLabelMap.CrmReturnEstimatedWeight} (${defaultWeightStr})" text=returnHeader.estimatedWeight/>
            <#else>
              <@inputTextRow title="${uiLabelMap.CrmReturnEstimatedWeight} (${defaultWeightStr})" name="estimatedWeight" default=returnHeader.estimatedWeight size=5/>
              <@inputHidden name="estimatedWeightUomId" value=defaultWeightUomId/>
            </#if>
          </tr>
          <@inputTextareaRow title=uiLabelMap.CommonComments name="comments" default=returnHeader.comments />
          <@inputSubmitRow title=uiLabelMap.CommonUpdate />
        <#else>
          <#if returnAddress?exists>
            <@displayRow title=uiLabelMap.CrmReturnedFrom text="${returnAddress.address1}, ${returnAddress.city}, ${returnAddress.stateName?if_exists} ${returnAddress.postalCode?if_exists} ${returnAddress.countryName?if_exists}" />
          </#if>
          <#if returnFacility?exists>
            <@displayRow title=uiLabelMap.CrmReturnedTo text=returnFacility.facilityName />
          </#if>
          <#if returnHeader.billingAccountId?exists>
            <@displayRow title=uiLabelMap.CrmStoreCreditAccount text="${billingAccount.description} (${billingAccount.billingAccountId})" />
          </#if>
          <#if returnService?exists>
            <@displayRow title=uiLabelMap.CrmReturnService text=returnService.description />
          <#else>
            <@displayRow title=uiLabelMap.CrmReturnService text=uiLabelMap.CrmReturnCustomerWillReturn />
          </#if>
          <#if returnShipment?exists && originTelecomNumber?has_content>
            <#assign phoneNumberText><#if originTelecomNumber.countryCode?has_content>${originTelecomNumber.countryCode}-</#if>${originTelecomNumber.areaCode?if_exists}-${originTelecomNumber.contactNumber?if_exists}</#assign>
            <@displayRow title=uiLabelMap.CrmPickupPhoneNumber text=phoneNumberText />
          </#if>
          <#if returnShipment?exists>
            <@displayLinkRow title=uiLabelMap.CrmReturnShipment text=returnShipment.shipmentId href="/warehouse/control/ViewShipment?shipmentId=${returnShipment.shipmentId}"/>
            <#if returnShipmentRouteSegment?exists>
              <@displayRow title=uiLabelMap.CrmReturnCarrierShipmentMethod text="${carrierName?if_exists} ${shipmentMethodDescription?if_exists}"/>
              <@displayRow title=uiLabelMap.CrmReturnShipmentTrackingCode text=returnShipmentRouteSegment.trackingIdNumber?if_exists/>
            </#if>
            <@displayRow title="${uiLabelMap.CrmReturnEstimatedWeight} (${defaultWeightStr})" text=returnHeader.estimatedWeight/>
          </#if>
          <@displayRow title=uiLabelMap.CommonComments text=returnHeader.comments />
        </#if>
    </table>
</form>

</div>

<#if returnHeader.statusId != "RETURN_CANCELLED">
  <#if returnItemsCreated>
    <#include "editReturnItemsForm.ftl"/>
    <br/>
  </#if>
  <#if returnHeader.statusId == "RETURN_REQUESTED">
    <#include "createReturnItemsForm.ftl"/>
  </#if>
</#if>
</#if>
