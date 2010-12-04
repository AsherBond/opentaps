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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<@import location="component://opentaps-common/webapp/common/order/infoMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#if order?exists>
      
<#if order.externalId?has_content>
  <#assign externalOrder = "(" + order.externalId + ")"/>
</#if>

<#-- Approve Order action; we need check permission when it is a purchase order -->
<#if (order.isCreated() || order.isProcessing() || order.isOnHold()) && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session))>
  <#assign actionAction><@actionForm form="approveOrderAction" text="${uiLabelMap.OrderApproveOrder}"/></#assign>
  <@form name="approveOrderAction" url="changeOrderItemStatus" orderId=order.orderId statusId="ITEM_APPROVED" />
<#-- Hold Order action; we need check permission when it is a purchase order -->
<#elseif (order.isApproved() && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session)))>
  <#assign actionAction><@actionForm form="holdOrderAction" text="${uiLabelMap.OrderHold}"/></#assign>
  <@form name="holdOrderAction" url="changeOrderStatus" orderId=order.orderId statusId="ORDER_HOLD" />
</#if>

<#if (order.isOpen() && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session))) >
  <#assign cancelOrderAction><@actionForm form="cancelOrderAction" text="${uiLabelMap.OpentapsCancelOrder}"/></#assign>
  <@form name="cancelOrderAction" url="cancelOrderItem" orderId="${order.orderId}" />
</#if>

<#-- Complete Order action; we need check permission when it is a purchase order -->
<#if order.isApproved() && !order.uncompleteItems?has_content && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session))>
  <#assign completeOptionAction><@actionForm form="completeOrderAction" text="${uiLabelMap.OrderCompleteOrder}"/></#assign>
  <@form name="completeOrderAction" url="changeOrderStatus" orderId=order.orderId statusId="ORDER_COMPLETED" />
</#if>
<#-- Order PDF action -->
<#assign pdfAction><@actionForm form="orderPdfAction" text="${uiLabelMap.OpentapsContentType_ApplicationPDF}"/></#assign>
<#if order.isSalesOrder()>
  <#assign orderReportId = "SALESORDER" />
<#else>
  <#assign orderReportId = "PRUCHORDER" />
</#if>
<@form name="orderPdfAction" target="_blank" method="get" url="order.pdf" orderId=order.orderId reportType="application/pdf" reportId="${orderReportId}"/>
<#-- Order Picklist PDF action -->
<#if order.isPickable()>
  <#assign picklistAction><@actionForm form="orderPicklistPdfAction" text="${uiLabelMap.ProductPickList}"/></#assign>
  <@form name="orderPicklistPdfAction" target="_blank" method="get" url="shipGroups.pdf" orderId=order.orderId />
</#if>
<#-- Order Email action -->
<#assign emailOrderAction><@actionForm form="orderEmailAction" text="${uiLabelMap.CommonEmail}"/></#assign>
<@form name="orderEmailAction" url="writeOrderEmail" orderId=order.orderId />
<#if actionAction?has_content || completeOptionAction?has_content>  
  <#assign separatorLineAction><option value="">${uiLabelMap.OpentapsDefaultActionSeparator}</option></#assign>
</#if>

<#assign extraOptions>
  <@selectActionForm name="orderActions" prompt="${uiLabelMap.CommonSelectOne}">
    ${actionAction?if_exists}
    ${cancelOrderAction?if_exists}
    ${completeOptionAction?if_exists}
    ${separatorLineAction?if_exists}
    ${picklistAction?if_exists}
    ${pdfAction?if_exists}
    ${emailOrderAction?if_exists}
  </@selectActionForm>
</#assign>

<@frameSection title="${uiLabelMap.OrderOrder} #${order.orderId} ${externalOrder?if_exists} ${uiLabelMap.CommonInformation}" extra=extraOptions>
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <#-- order name -->
      <#if order.isOpen()>
        <@form name="updateOrdrNameHiddenForm" url="updateOrderHeader" orderId=order.orderId>
          <@infoRowNested title=uiLabelMap.OrderOrderName >
            <@inputText name="orderName" default=order.orderName />
            <@inputSubmit title=uiLabelMap.CommonUpdate />
          </@infoRowNested>
        </@form>
      <#else>
        <@infoRow title=uiLabelMap.OrderOrderName content=order.orderName?default("") /> 
      </#if>
      <@infoSepBar/>
      <#-- order date --> 
      <@infoRow title=uiLabelMap.OrderDateOrdered content=order.orderDate />
      <@infoSepBar/>
      <#-- order status history -->
      <@infoRowNested title=uiLabelMap.OrderStatusHistory>
        <div class="tabletext">${uiLabelMap.OrderCurrentStatus}: ${order.status.get("description",locale)}</div>
        <#if order.orderStatuses?has_content>
          <hr class="sepbar"/>
          <#list order.orderStatuses as orderStatus>
            <div class="tabletext">
              ${getLocalizedDate(orderStatus.statusDatetime, "DATE_TIME")}: ${orderStatus.statusItem.get("description",locale)} ${uiLabelMap.CommonBy} ${orderStatus.statusUserLogin?default("unknown")}</div>
          </#list>
        </#if>
      </@infoRowNested>
      <#if order.internalCode?has_content>
        <@infoSepBar/>
        <@infoRow title=uiLabelMap.OrderInternalCode content=order.internalCode />
      </#if>
      <#if order.primaryPoNumber?has_content>
        <@infoSepBar/>
        <@infoRow title=uiLabelMap.OpentapsPONumber content=order.primaryPoNumber />
      </#if>
      <#-- sales channel is only for sales order -->
      <#if order.isSalesOrder()>
        <@infoSepBar/>
        <#if order.salesChannelEnumId?has_content>
          <@infoRow title=uiLabelMap.OrderSalesChannel content=(order.salesChannel.get("description",locale))?default(uiLabelMap.CommonNA) />
        <#else/>
          <@infoRow title=uiLabelMap.OrderSalesChannel content=uiLabelMap.CommonNA />
        </#if>
      </#if>

      <#if order.commissionAgents?has_content>
        <@infoSepBar/>
        <@infoRowNested title=uiLabelMap.CrmCommissionRep>
          <div class="tabletext">
            <#list order.commissionAgents as party>
              ${party.name}<#if party_has_next><br/></#if>
            </#list>
          </div>
        </@infoRowNested>
      </#if>

      <#if order.distributorOrderRole?exists>
        <@infoSepBar/>
        <@infoRowNested title=uiLabelMap.OrderDistributor>
          <div class="tabletext">
            ${order.distributor.getNameForDate(order.orderDate).fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
          </div>
        </@infoRowNested>
      </#if>

      <#if order.affiliateOrderRole?exists>
        <@infoSepBar/>
        <@infoRowNested title=uiLabelMap.OrderAffiliate>
          <div class="tabletext">
            ${order.affiliate.getNameForDate(order.orderDate).fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
          </div>
        </@infoRowNested>
      </#if>

      <#if orderContentWrapper.get("IMAGE_URL")?has_content>
        <@infoSepBar/>
        <@infoRowNested title=uiLabelMap.OrderImage>
          <div class="tabletext">
            <a href="<@ofbizUrl>viewimage?orderId=${order.orderId}&orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
          </div>
        </@infoRowNested>
      </#if>

    </table>
</@frameSection>

</#if> <#-- end of if order?exists -->
