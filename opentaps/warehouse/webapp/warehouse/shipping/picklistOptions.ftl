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

<#macro buttonsAndPrinter formName>
    <a class="buttontext" href="javascript:document.forms['${formName}'].submit()">${uiLabelMap.ProductCreatePicklist}</a>
    <a class="buttontext" href="javascript:document.forms['${formName}'].action='<@ofbizUrl>createAndPrintPicklistFromOrders</@ofbizUrl>';document.forms['${formName}'].submit()">${uiLabelMap.WarehouseCreateAndPrint}</a>
    <span class="tabletext">${uiLabelMap.WarehousePrinter}:</span>&nbsp;
    <select name="printerName" class="selectBox">
       <#list printers?default([]) as printer>
           <option value="${printer}" ${(defaultPrinter == printer)?string("selected=\"selected\"","")}>${printer}</option>
       </#list>
    </select>
</#macro>

<#-- when maxNumberOfOrders is given, the reported number will be maxed by it and not the actual total, unless to total returned in < max -->
<#assign listSizeApprox = ""/>
<#if parameters.maxNumberOfOrders?exists && (!nReturnedOrders?has_content || nReturnedOrders?string == parameters.maxNumberOfOrders)>
  <#assign listSizeApprox = "&gt;= "/>
</#if>

<#-- used as default value for the picklist creation forms -->
<#assign defaultPicklistSize = 20 />
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.ProductFindOrdersToPick}</div>
    </div>
    <div class="screenlet-body">
        <table class="basic-table">
            <tr>
                <th>${uiLabelMap.ProductShipmentMethod}</th>
                <th>${uiLabelMap.ProductReadyToPick}</th>
                <th>${uiLabelMap.ProductNeedStockMove}</th>
                <th>&nbsp;</th>
            </tr>
            <#if rushOrderInfo?has_content>
                <#assign orderReadyToPickInfoList = rushOrderInfo.orderReadyToPickInfoList?if_exists>
                <#assign orderNeedsStockMoveInfoList = rushOrderInfo.orderNeedsStockMoveInfoList?if_exists>
                <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                <tr>
                    <td>[Rush Orders, all Methods]</td>
                    <td>${listSizeApprox}${orderReadyToPickInfoListSize}</td>
                    <td>${orderNeedsStockMoveInfoListSize}</td>
                    <td>
                        <#if orderReadyToPickInfoList?has_content>
                            <form method="post" name="createPicklistRush" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                <@inputHidden name="facilityId" value=facilityId />
                                <@inputHidden name="isRushOrder" value="Y" />
                                ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                                <@buttonsAndPrinter formName="createPicklistRush"/>
                            </form>
                        <#else>
                            &nbsp;
                        </#if>
                    </td>
                </tr>
            </#if>
            <#if pickMoveByShipmentMethodInfoList?has_content>
                <#assign orderReadyToPickInfoListSizeTotal = 0>
                <#assign orderNeedsStockMoveInfoListSizeTotal = 0>
                <#list pickMoveByShipmentMethodInfoList as pickMoveByShipmentMethodInfo>
                    <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists>
                    <#assign orderReadyToPickInfoList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists>
                    <#assign orderNeedsStockMoveInfoList = pickMoveByShipmentMethodInfo.orderNeedsStockMoveInfoList?if_exists>
                    <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                    <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                    <#assign orderReadyToPickInfoListSizeTotal = orderReadyToPickInfoListSizeTotal + orderReadyToPickInfoListSize>
                    <#assign orderNeedsStockMoveInfoListSizeTotal = orderNeedsStockMoveInfoListSizeTotal + orderNeedsStockMoveInfoListSize>
                    <tr>
                        <td><a href="<@ofbizUrl>PicklistOptions?viewDetail=${shipmentMethodType.shipmentMethodTypeId?if_exists}&facilityId=${facilityId?if_exists}&maxNumberOfOrders=${parameters.maxNumberOfOrders?if_exists}</@ofbizUrl>" class="linktext"><#if shipmentMethodType?exists && shipmentMethodType?has_content>${shipmentMethodType.description}<#else>${groupName?if_exists}</#if></a></td>
                        <td>${listSizeApprox}${orderReadyToPickInfoListSize}</td>
                        <td>${orderNeedsStockMoveInfoListSize}</td>
                        <td>
                            <#if orderReadyToPickInfoList?has_content>
                                <form method="post" name="createPicklist_${pickMoveByShipmentMethodInfo_index}" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                    <@inputHidden name="facilityId" value=facilityId />
                                    <#if shipmentMethodType?exists && shipmentMethodType?has_content>
                                    <@inputHidden name="shipmentMethodTypeId" value=shipmentMethodType.shipmentMethodTypeId />
                                    <#else>
                                        <@inputHidden name="orderIdList" value="" />
                                        <#assign orderIdsForPickList = orderReadyToPickInfoList?if_exists>
                                        <#list orderIdsForPickList as orderIdForPickList>
                                            <@inputHidden name="orderIdList" value=orderIdForPickList.orderHeader.orderId />
                                        </#list>
                                    </#if>
                                    ${uiLabelMap.ProductPickFirst}:<input type="text" class="inputBox" size="4" name="maxNumberOfOrders" value="${defaultPicklistSize}"/>
                                    <@buttonsAndPrinter formName="createPicklist_${pickMoveByShipmentMethodInfo_index}"/>
                                </form>
                            <#else>
                                &nbsp;
                            </#if>
                        </td>
                    </tr>
                </#list>
                <tr>
                    <th>${uiLabelMap.CommonAllMethods}</div></th>
                    <th>${listSizeApprox}${orderReadyToPickInfoListSizeTotal}</div></th>
                    <th>${orderNeedsStockMoveInfoListSizeTotal}</div></th>
                    <td>
                      <#if (orderReadyToPickInfoListSizeTotal > 0)>
                        <form method="post" name="createPicklistAll" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                            <@inputHidden name="facilityId" value=facilityId />
                            ${uiLabelMap.ProductPickFirst}:<input type="text" class="inputBox" size="4" name="maxNumberOfOrders" value="${defaultPicklistSize}"/>
                            <@buttonsAndPrinter formName="createPicklistAll"/>
                        </form>
                      <#else>
                        &nbsp;
                      </#if>
                    </td>
                </tr>
            <#else>
                <tr><td colspan="4"><div class="head3">${uiLabelMap.ProductNoOrdersFoundReadyToPickOrNeedStockMoves}.</div></td></tr>
            </#if>
        </table>
    </div>
</div>
<br/>

<#assign viewDetail = requestParameters.viewDetail?if_exists/>
<#if viewDetail?has_content>
    <#list pickMoveByShipmentMethodInfoList as pickMoveByShipmentMethodInfo>
        <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists/>
        <#if shipmentMethodType?if_exists.shipmentMethodTypeId == viewDetail>
            <#assign toPickList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists/>
        </#if>                
    </#list>
</#if>

<#if toPickList?has_content>
  <div class="screenlet">
    <div class="screenlet-header">
      <div class="boxhead">${shipmentMethodType.description?if_exists} Detail <#if parameters.maxNumberOfOrders?has_content >(First ${parameters.maxNumberOfOrders} orders)</#if> </div>
    </div>
    <div class="screenlet-body">
      <table class="basic-table">
        <tr>
          <th>${uiLabelMap.OrderOrderId}</th>
          <th>${uiLabelMap.OrderDate}</th>
          <th>${uiLabelMap.CrmSalesChannel}</th>
          <th>${uiLabelMap.OpentapsItemID}</th>
          <th>${uiLabelMap.CommonDescription}</th>
          <th>${uiLabelMap.OrderShipGroup}</th>
          <th>${uiLabelMap.CommonQuantity}</th>
        </tr>
        <#list toPickList as toPick>
          <#assign oiasgal = toPick.orderItemAndShipGroupAssocList/>
          <#assign header = toPick.orderHeader/>
          <#assign channel = header.getRelatedOne("SalesChannelEnumeration")?if_exists/>

          <#list oiasgal as oiasga>
            <#assign reservations = oiasga.getRelated("OrderItemShipGrpInvResAndItem")/>
            <#assign qty = 0/>
            <#list reservations as res>
              <#if facilityId.equals(res.facilityId) >
                <#assign qty = qty + res.quantity />
              </#if>
            </#list>
            <#if qty gt 0>
              <#assign product = oiasga.getRelatedOne("Product")?if_exists/>
              <tr>
                <@displayLinkCell href="/crmsfa/control/orderview?orderId=${oiasga.orderId}" text=oiasga.orderId />
                <@displayDateCell date=header.orderDate />
                <@displayCell text=(channel.description)! />
                <@displayCell text=oiasga.orderItemSeqId />
                <@displayLinkCell href="/catalog/control/EditProduct?productId=${oiasga.productId?if_exists}" text=(product.internalName)! />
                <@displayCell text=oiasga.shipGroupSeqId />
                <@displayCell text=qty />
              </tr>
            </#if>
          </#list>
          <tr>
            <td colspan="7"><hr/></td>
          </tr>
        </#list>
      </table>
    </div>
  </div>
</#if>
