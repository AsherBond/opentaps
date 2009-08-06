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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<@import location="component://opentaps-common/webapp/common/order/infoMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->

<#if order?exists>
      
<#if order.externalId?has_content>
  <#assign externalOrder = "(" + order.externalId + ")"/>
</#if>

<#-- we need check permission when it is a purchase order -->
<#if (order.isCreated() || order.isProcessing() || order.isOnHold()) && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session))>
  <#assign actionAction><@action url="changeOrderItemStatus?statusId=ITEM_APPROVED&amp;${paramString}" text="${uiLabelMap.OrderApproveOrder}"/></#assign>
<#-- we need check permission when it is a purchase order -->
<#elseif (order.isApproved() && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session)))>
  <#assign actionAction><@action url="changeOrderStatus?statusId=ORDER_HOLD&amp;${paramString}" text="${uiLabelMap.OrderHold}"/></#assign>
</#if>

<#-- we need check permission when it is a purchase order -->
<#if order.isApproved() && !order.uncompleteItems?has_content && (order.isSalesOrder() || security.hasEntityPermission("PRCH", "_ORD_APPRV", session))>
  <#assign completeOptionAction><@action url="changeOrderStatus?orderId=${order.orderId}&amp;statusId=ORDER_COMPLETED" text="${uiLabelMap.OrderCompleteOrder}"/></#assign>
</#if>
<#assign pdfAction><@action url="order.pdf?orderId=${order.orderId}" text="${uiLabelMap.OpentapsContentType_ApplicationPDF}"/></#assign>
<#if order.isPickable()>
  <#assign picklistAction><@action url="shipGroups.pdf?orderId=${order.orderId}" text="${uiLabelMap.ProductPickList}"/></#assign>
</#if>
  <#assign emailOrderAction><@action url="writeOrderEmail?orderId=${order.orderId}" text="${uiLabelMap.CommonEmail}"/></#assign>
<#if actionAction?has_content || completeOptionAction?has_content>  
  <#assign separatorLineAction><@action url="" text="${uiLabelMap.OpentapsDefaultActionSeparator}"/></#assign>
</#if>
<div class="screenlet">
  <div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.OrderOrder} #${order.orderId} ${externalOrder?if_exists} ${uiLabelMap.CommonInformation}</div>
    
  <div class="subMenuBar">
    <@selectAction name="myProfileContactMech" prompt="${uiLabelMap.CommonSelectOne}">
      ${actionAction?if_exists}
      ${completeOptionAction?if_exists}
      ${separatorLineAction?if_exists}
      ${picklistAction?if_exists}
      ${pdfAction?if_exists}
      ${emailOrderAction?if_exists}
    </@selectAction>
  </div>
    
    <div class="subMenuBar">${picklistLink?if_exists}${pdfLink?if_exists}${emailOrderLink?if_exists}${actionLink?if_exists}${completeOption?if_exists}</div>
  </div>
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="1" cellspacing="0">
      <#-- order name -->
      <#if order.orderName?has_content>
        <@infoRow title=uiLabelMap.OrderOrderName content=order.orderName /> 
        <@infoSepBar/> 
      </#if>
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
  </div>
</div>

</#if> <#-- end of if order?exists -->
