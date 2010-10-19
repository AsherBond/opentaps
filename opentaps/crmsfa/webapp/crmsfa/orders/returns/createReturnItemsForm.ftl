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

<#-- This file has been modified by Open Source Strategies, Inc.
     It was originally returnItemInc.ftl.
-->

<#assign selectAllFormName = "returnItems"/>

<@frameSectionHeader title=uiLabelMap.CrmSelectItemsToReturn />
<div class="form">
<form name="returnItems" method="post" action="<@ofbizUrl>createReturnItems</@ofbizUrl>">
  <input type="hidden" name="returnId" value="${returnId}">
  <input type="hidden" name="_useRowSubmit" value="Y">

  <table border="0" width="100%" cellpadding="2" cellspacing="0">
    <tr>
      <td colspan="7"><div class="head3">${uiLabelMap.OrderReturnFromOrder} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="linktext">${orderId}</div></td>
      <td align="right" colspan="8">
        <span class="tableheadtext">${uiLabelMap.CommonSelectAll}</span>&nbsp;
        <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, '${selectAllFormName}');"/>
      </td>
    </tr>

    <tr>
      <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.OrderOrderQty}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.OrderReturnQty}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.OrderUnitPrice}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.OrderReturnPrice}*</div></td>
      <td><div class="tableheadtext">${uiLabelMap.OrderReturnReason}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonProcessing}</div></td>
      <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderInclude}?</div></td>
    </tr>
    <tr><td colspan="8"><hr class="sepbar"></td></tr>
    <#if returnableItems?has_content>
      <#assign rowCount = 0>
      <#list returnableItems.keySet() as orderItem>
        <#assign key = orderItem.orderItemTypeId?default("") />
        <#assign returnItemType = returnItemTypeMap.get(key)?default("") />
        <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="${returnItemType}"/>
        <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
        <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
        <input type="hidden" name="description_o_${rowCount}" value="${orderItem.itemDescription?if_exists}"/>

        <#-- need some order item information -->
        <#assign orderHeader = orderItem.getRelatedOne("OrderHeader")>
        <#assign itemCount = orderItem.quantity>
        <#assign itemPrice = orderItem.unitPrice>
        <#-- end of order item information -->

        <tr>
          <td>
            <div class="tabletext">
              <#if orderItem.productId?exists>
              <b>${orderItem.productId}</b>:&nbsp;
              <input type="hidden" name="productId_o_${rowCount}" value="${orderItem.productId}">
              </#if>
              ${orderItem.itemDescription}
            </div>
          </td>
          <td align='center'>
            <div class="tabletext">${orderItem.quantity?string.number}</div>
          </td>
          <td>
            <input type="text" class="inputBox" size="4" name="returnQuantity_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnableQuantity")}"/>
          </td>
          <td align='left'>
            <div class="tabletext"><@ofbizCurrency amount=orderItem.unitPrice isoCode=orderHeader.currencyUom/></div>
          </td>
          <td>
            <input type="text" class="inputBox" size="8" name="returnPrice_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnablePrice")?string("##0.00")}"/>
          </td>
          <td>
            <select name="returnReasonId_o_${rowCount}" class="selectBox">
              <#list returnReasons as reason>
              <option value="${reason.returnReasonId}">${reason.get("description",locale)?default(reason.returnReasonId)}</option>
              </#list>
            </select>
          </td>
          <td>
            <select name="returnTypeId_o_${rowCount}" class="selectBox">
              <#list returnTypes as type>
              <option value="${type.returnTypeId}" <#if type.returnTypeId=="RTN_REFUND">selected</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
              </#list>
            </select>
          </td>
          <td align="right">
            <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
          </td>
        </tr>
        <#assign rowCount = rowCount + 1>
      </#list>
    </table>

  <table border="0" width="100%" cellpadding="2" cellspacing="0">
    <tr><td colspan="4"><hr class="sepbar"></td></tr>
    <tr>
      <td colspan="4"><div class="head3">${uiLabelMap.OrderReturnAdjustments} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="linktext">${orderId}</div></td>
    </tr>
    <tr><td colspan="4"><hr class="sepbar"></td></tr>
    <#if orderHeaderAdjustments?has_content>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonAmount}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.OrderReturnType}</div></td>
        <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderInclude}?</div></td>
      </tr>
      <tr><td colspan="4"><hr class="sepbar"></td></tr>
      <#list orderHeaderAdjustments as adj>
        <#assign returnAdjustmentType = returnItemTypeMap.get(adj.get("orderAdjustmentTypeId"))/>
        <#assign adjustmentType = adj.getRelatedOne("OrderAdjustmentType")/>
        <#assign description = adj.description?default(adjustmentType.get("description",locale))/>

        <input type="hidden" name="returnAdjustmentTypeId_o_${rowCount}" value="${returnAdjustmentType}"/>
        <input type="hidden" name="orderAdjustmentId_o_${rowCount}" value="${adj.orderAdjustmentId}"/>
        <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="_NA_"/>
        <input type="hidden" name="description_o_${rowCount}" value="${description}"/>
        <input type="hidden" name="expectedItemStatus_o_${rowCount}" value="INV_RETURNED"/>
        <tr>
          <td>
            <div class="tabletext">
              ${description?default("N/A")}
            </div>
          </td>
          <td>
            <#if adj.amount?has_content>
              <@displayCurrency currencyUomId=orderHeader.currencyUom amount=adj.amount />
            <#else>
              <input type="text" class="inputBox" size="8" name="amount_o_${rowCount}" <#if adj.amount?has_content>value="${adj.amount?string("##0.00")}"</#if>/>
            </#if>
          </td>
          <td>
            <select name="returnTypeId_o_${rowCount}" class="selectBox">
              <#list returnTypes as type>
              <option value="${type.returnTypeId}" <#if type.returnTypeId == "RTN_REFUND">selected</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
              </#list>
            </select>
          </td>

          <td align="right">
            <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
          </td>
        </tr>
        <#assign rowCount = rowCount + 1>
      </#list>
    <#else>
      <tr><td colspan="4"><div class="tableheadtext">${uiLabelMap.OrderNoOrderAdjustments}</div></td></tr>
    </#if>

    <#assign manualAdjRowNum = rowCount/>
    <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="RET_MAN_ADJ"/>
    <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="_NA_"/>
    <tr><td colspan="4"><hr class="sepbar"></td></tr>
    <tr>
      <td colspan="4">
        <div class="head3">${uiLabelMap.OrderReturnManualAdjustment} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="linktext">${orderId}</div></td></div>
      </td>
    </tr>
    <tr>
      <td>
        <input type="text" class="inputBox" size="30" name="description_o_${rowCount}">
      </td>
      <td>
        <input type="text" class="inputBox" size="8" name="amount_o_${rowCount}" value="${0.00?string("##0.00")}"/>
      </td>
      <td>
        <select name="returnTypeId_o_${rowCount}" class="selectBox">
          <#list returnTypes as type>
            <option value="${type.returnTypeId}" <#if type.returnTypeId == "RTN_REFUND">selected</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
          </#list>
        </select>
      </td>
      <td align="right">
        <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
      </td>
    </tr>
    <#assign rowCount = rowCount + 1>

    <!-- final row count -->
    <input type="hidden" name="_rowCount" value="${rowCount}"/>

     <tr>
       <td colspan="4" align="right">
         <a href="javascript:document.${selectAllFormName}.submit()" class="buttontext">${uiLabelMap.OrderReturnSelectedItems}</a>
       </td>
     </tr>
   <#else>
     <tr><td colspan="4"><div class="tabletext">${uiLabelMap.OrderReturnNoReturnableItems} #${orderId}</div></td></tr>
   </#if>
   <tr>
     <td colspan="4"><div class="tabletext">*${uiLabelMap.OrderReturnPriceNotIncludeTax}</div></td>
   </tr>
   </table>

</form>
</div>
