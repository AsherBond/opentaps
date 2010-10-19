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

<#if order?has_content>

<#macro sepBar span=8>
  <td colspan="${span}"><hr class="sepbar"/></td>
</#macro>

<#macro sepBarRow span=8>
  <tr><@sepBar span=span/></tr>
</#macro>

<#macro sepRow span=8>
  <tr><td colspan="${span}">&nbsp;</td></tr>
</#macro>

<#macro sectionSepBar>
  <tr><td colspan="2">&nbsp;</td><@sepBar span=3/></tr>
</#macro>

<#macro subtotalRow title subtotal>
  <tr>
    <td align="right" colspan="4"><div class="tabletext"><b>${title}&nbsp;:</b></div></td>
    <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=subtotal isoCode=order.currencyUom/></div></td>
  </tr>
</#macro>

<#macro editAdjustmentLine adjustment>
  <#assign adjustmentAmount = adjustment.calculateAdjustment(order)/>
  <#if adjustmentAmount != 0>
    <@form name="deleteOrderAdjustmentForm_${adjustment.orderAdjustmentId}" url="deleteOrderAdjustment" orderId=order.orderId orderAdjustmentId=adjustment.orderAdjustmentId />
    <@form name="updateOrderAdjustmentForm_${adjustment.orderAdjustmentId}" url="updateOrderAdjustment" orderId=order.orderId orderAdjustmentId=adjustment.orderAdjustmentId>
      <tr>
        <td align="right" colspan="3">
          <div class="tabletext"><b>${adjustment.type.get("description",locale)}</b> ${adjustment.comments?if_exists} :
            <input type="text" name="description" value="${adjustment.description?if_exists}" size="40" maxlength="60" class="inputBox"/></div>
        </td>
        <td align="right" nowrap="nowrap">
          <div class="tabletext"><input type="text" name="amount" size="10" value="<@ofbizAmount amount=adjustmentAmount/>" class="inputBox"/></div>
        </td>
        <td align="let" colspan="2">
          <@submitFormLink form="updateOrderAdjustmentForm_${adjustment.orderAdjustmentId}" text=uiLabelMap.CommonUpdate/>
          <@submitFormLink form="deleteOrderAdjustmentForm_${adjustment.orderAdjustmentId}" text=uiLabelMap.CommonDelete/>
        </td>
      </tr>
    </@form>
  </#if>
</#macro>

<#macro adjustmentSection title ajdList total>
  <#list ajdList as adj>
    <@editAdjustmentLine adjustment=adj/>
  </#list>
  <@sepRow/>
  <@subtotalRow title=title subtotal=total />
</#macro>

<div class="screenlet">
  <@sectionHeader title=uiLabelMap.OrderOrderItems>
    <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
      <#if order?has_content && !order.isCancelled() && !order.isCompleted()>
        <@form name="cancelOrderItemForm" url="cancelOrderItem" orderId="${order.orderId}" orderItemSeqId="" shipGroupSeqId=""/>
        <div class="subMenuBar"><@submitFormLink form="cancelOrderItemForm" text=uiLabelMap.OrderCancelAllItems class="subMenuButton"/><a href="<@ofbizUrl>orderview?${paramString}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.OrderViewOrder}</a></div>
      </#if>
    </#if>
  </@sectionHeader>
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="0" cellspacing="0">
      <#if !order.items?has_content>
        <tr><td><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
      <#else>
        <form name="updateItemInfo" method="post" action="<@ofbizUrl>updateOrderItems</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${order.orderId}"/>
          <tr align="left" valign="bottom">
            <td width="60%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
            <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderUnitPrice}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderAdjustments}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
            <td width="5%">&nbsp;</td>
          </tr>
          <#list order.items as item>
           <#if !(item.isCancelled() && item.isPromo())>
            <@sepBarRow/>
            <tr>
              <#if item.productId?exists && item.productId == "shoppingcart.CommentLine">
                <td colspan="1" valign="top">
                  <b><div class="tabletext"> &gt;&gt; ${item.itemDescription}</div></b>
                </td>
              <#else>
                <#-- Product and item description -->
                <td valign="top" colspan="1">
                  <div class="tabletext">
                    <#if order.isCancelled() || order.isCompleted()>
                      <#if item.productId?exists>
                        ${item.productId?default("N/A")} - ${item.itemDescription?if_exists}
                      <#elseif order.type?exists>
                        ${item.type.description} - ${item.itemDescription?if_exists}
                      <#else>
                        ${item.itemDescription?if_exists}
                      </#if>
                    <#else>
                      <#if item.productId?exists>
                        <a href="<@ofbizUrl>product/~product_id=${item.productId}</@ofbizUrl>" target="_blank" class="linktext">${item.productId?default("N/A")}</a>: 
                      <#elseif item.type?exists>
                        ${item.type.description}
                      </#if>
                      <input class="inputBox" type="text" size="50" name="idm_${item.orderItemSeqId}" value="${item.itemDescription?if_exists}"/>
                    </#if>
                  </div>
                </td>
                    
                <#-- Quantity -->
                <td align="right" valign="top" nowrap="nowrap">
                  <div class="tabletext">${item.orderedQuantity}&nbsp;&nbsp;</div>
                </td>

                <#-- Unit Price -->
                <td align="right" valign="top" nowrap="nowrap">
                  <div class="tabletext">
                    <input class="inputBox" type="text" size="8" name="ipm_${item.orderItemSeqId}" value="<@ofbizAmount amount=item.unitPrice/>"/>
                    &nbsp;<input type="checkbox" name="opm_${item.orderItemSeqId}" value="Y"/>
                  </div>
                </td>

                <#-- Item Adjustments -->
                <td align="right" valign="top" nowrap="nowrap">
                  <div class="tabletext"><@ofbizCurrency amount=item.otherAdjustmentsAmount isoCode=order.currencyUom/></div>
                </td>

                <#-- Item Sub total -->
                <td align="right" valign="top" nowrap="nowrap">
                  <#if !item.isCancelled()>
                    <div class="tabletext"><@ofbizCurrency amount=item.subTotal isoCode=order.currencyUom/></div>
                  <#else>
                    <div class="tabletext"><@ofbizCurrency amount=0.00 isoCode=order.currencyUom/></div>
                  </#if>
                </td>

                <#-- Item Cancel button -->
                <td>&nbsp;</td>
                <td align="right" valign="top" nowrap="nowrap">
                  <#if (!item.isCancelled() && !item.isCompleted()) && (security.hasEntityPermission("ORDERMGR", "_ADMIN", session) || (security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && !order.isSent()))>
                    <div class="tabletext">
                      <@submitFormLink form="cancelOrderItemForm" text=uiLabelMap.CommonCancelAll orderItemSeqId=item.orderItemSeqId />
                    </div>
                  <#else>
                    &nbsp;
                  </#if>
                </td>
              </#if>
            </tr>
                
            <#-- now show adjustment details per line item -->
            <#list item.adjustments as orderItemAdjustment>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;">
                    <b><i>${uiLabelMap.OrderAdjustment}</i>:</b>
                    <b>${orderItemAdjustment.type.get("description",locale)}</b>:
                    ${orderItemAdjustment.description?if_exists}
                    <#if orderItemAdjustment.comments?has_content> (${orderItemAdjustment.comments})</#if>
                      
                    <#if orderItemAdjustment.isSalesTax()>
                      <#if orderItemAdjustment.primaryGeoId?has_content>
                        <b>${uiLabelMap.OrderJurisdiction}:</b> ${orderItemAdjustment.primaryGeo.geoName} [${orderItemAdjustment.primaryGeo.abbreviation?if_exists}]
                        <#if orderItemAdjustment.secondaryGeoId?has_content>
                          (<b>in:</b> ${orderItemAdjustment.secondaryGeo.geoName} [${orderItemAdjustment.secondaryGeo.abbreviation?if_exists}])
                        </#if>
                      </#if>
                      <#if orderItemAdjustment.sourcePercentage?exists><b>Rate:</b> ${orderItemAdjustment.sourcePercentage}</#if>
                        <#if orderItemAdjustment.customerReferenceId?has_content><b>Customer Tax ID:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                        <#if orderItemAdjustment.exemptAmount?exists><b>Exempt Amount:</b> ${orderItemAdjustment.exemptAmount}</#if>
                    </#if>
                  </div>
                </td>
                <td colspan="2">&nbsp;</td>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;">
                    <@ofbizCurrency amount=orderItemAdjustment.calculateAdjustment(item) isoCode=order.currencyUom/>
                  </div>
                </td>
                <td colspan="3">&nbsp;</td>
              </tr>
            </#list>
                
            <#-- now show ship group info per line item -->
            <#if item.orderItemShipGroupAssocs?has_content>
              <@sepRow span=7/>
              <#list item.orderItemShipGroupAssocs as shipGroupAssoc>
                <#assign shipGroup = shipGroupAssoc.orderItemShipGroup />
                <#assign shipGroupAddress = shipGroup.postalAddress?if_exists/>
                <#assign shipGroupQty = shipGroupAssoc.quantity - shipGroupAssoc.cancelQuantity?default(0) />
                <#if shipGroupQty gt 0>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;"><input type="text" class="inputBox" name="iqm_${shipGroupAssoc.orderItemSeqId}:${shipGroupAssoc.shipGroupSeqId}" size="6" value="${shipGroupQty}"/></div>
                    </td>
                    <td colspan="3">&nbsp;</td>
                    <td align="right" valign="top" nowrap="nowrap">
                      <#assign itemStatusOkay = !item.isCancelled() && !item.isCompleted()/>
                      <#if itemStatusOkay && (security.hasEntityPermission("ORDERMGR", "_ADMIN", session) || (security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && !order.isSent()))>
                        <div class="tabletext">
                          <@submitFormLink form="cancelOrderItemForm" text=uiLabelMap.CommonCancel orderItemSeqId=item.orderItemSeqId shipGroupSeqId=shipGroup.shipGroupSeqId/>
                        </div>
                      <#else>
                        &nbsp;
                      </#if>
                    </td>
                  </tr>
                </#if>
              </#list>
            </#if>

            <#if !item.isPromo()>
            <#-- accounting tags -->
            <tr>
              <td colspan="2" valign="top">
                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                  <#if tagTypes?has_content>
                    <@accountingTagsSelectRows tags=tagTypes entity=item suffix="_${item.orderItemSeqId}"/>
                  </#if>
                </table>
              </td>
            </tr>
            </#if>

           </#if>
          </#list> <#-- end of listing orderItems -->

          <@sectionSepBar/>
          <#-- item sub total -->
          <@subtotalRow title=uiLabelMap.OrderItemsSubTotal subtotal=order.itemsSubTotal />

          <#-- tax adjustments -->
          <@subtotalRow title=uiLabelMap.OrderTotalSalesTax subtotal=order.taxAmount />

          <tr>
            <td align="right" colspan="3">
              <@inputHidden name="forceComplete" value="Y"/>
              <@inputIndicator name="recalcAdjustments" default="Y" yesLabel="Recalculate adjustments" noLabel="Do not recalculate adjustments" /><br/>
              <@inputSubmit title=uiLabelMap.OrderUpdateItems />
            </td>
          </tr>
          <@sepBarRow/>
        </form>
      </#if> <#-- !orderItemList?has_content -->

      <#-- shipping adjustments -->
      <@adjustmentSection title=uiLabelMap.OrderTotalShippingAndHandling ajdList=order.shippingAdjustments total=order.shippingAmount/>
      <@sepBarRow/>

      <#-- other adjustments -->
      <@adjustmentSection title=uiLabelMap.OrderTotalOtherOrderAdjustments ajdList=order.nonShippingAdjustments total=order.otherAdjustmentsAmount/>

      <#-- add new adjustment -->
      <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && !order.isCompleted() && !order.isCancelled() && !order.isRejected() >
        <@sepBarRow/>
        <@sepRow/>
        <form name="addAdjustmentForm" method="post" action="<@ofbizUrl>createOrderAdjustment</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${order.orderId}"/>
          <input type="hidden" name="comments" value="Added manually by [${userLogin.userLoginId}]"/>
          <tr>
            <td align="right" colspan="3">
              <span class="tableheadtext">${uiLabelMap.OrderAdjustment} :</span>
              <select name="orderAdjustmentTypeId" class="selectBox">
                <#list orderAdjustmentTypes as type>
                  <option value="${type.orderAdjustmentTypeId}">${type.get("description",locale)?default(type.orderAdjustmentTypeId)}</option>
                </#list>
              </select>
              <select name="shipGroupSeqId" class="selectBox">
                <option value="_NA_"></option>
                <#list order.shipGroups as shipGroup>
                  <option value="${shipGroup.shipGroupSeqId}">${uiLabelMap.OrderShipGroup} ${shipGroup.shipGroupSeqId}</option>
                </#list>
              </select>
              <input type="text" name="description" value="" size="30" maxlength="60" class="inputBox"/>
            </td>
            <td align="right" colspan="1">
              <input type="text" name="amount" size="6" value="<@ofbizAmount amount=0.00/>" class="inputBox"/>
            </td>
            <td align="left" colspan="1">
              <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/>
            </td>
          </tr>
        </form>
      </#if>

      <@sectionSepBar/>
      <#-- grand total -->
      <@subtotalRow title=uiLabelMap.OrderOrderTotal subtotal=order.total />
      <@subtotalRow title=uiLabelMap.OrderTotalDue subtotal=order.openAmount />

    </table>
  </div>
</div>

</#if>
