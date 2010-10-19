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
     It was originally returnItems.ftl
-->

<#macro displayReturnAdjustment returnAdjustment adjEditable>
    <#assign returnHeader = returnAdjustment.getRelatedOne("ReturnHeader")/>
    <#assign adjReturnType = returnAdjustment.getRelatedOne("ReturnType")?if_exists/>
    <input type="hidden" name="_rowSubmit_o_${rowCount}" value="Y" />
    <input type="hidden" name="returnAdjustmentId_o_${rowCount}" value="${returnAdjustment.returnAdjustmentId}" />
    <tr class="tabletext">
        <td>&nbsp;</td>
        <td colspan="3" class="tabletext">${returnAdjustment.get("description",locale)?default("N/A")}
            <#if returnAdjustment.comments?has_content>: ${returnAdjustment.comments}</#if>
        </div></td>
        <#if (adjEditable)>
           <td align="right">
              <input type="text" class="inputBox" size="8" name="amount_o_${rowCount}" value="${returnAdjustment.amount?default(0)?string("##0.00")}"/>
           </td>
        <#else>
           <td class="tabletextright"><@ofbizCurrency amount=returnAdjustment.amount?default(0) isoCode=returnHeader.currencyUomId/></td>
        </#if>
        <td>&nbsp;</td>
        <td><div class="tabletext">
           <#if (!adjEditable)>
               ${adjReturnType.description?default("${uiLabelMap.CommonNA}")}
           <#else>
               <select name="returnTypeId_o_${rowCount}" class="selectBox">
                  <#if (adjReturnType?has_content)>
                    <option value="${adjReturnType.returnTypeId}">${adjReturnType.get("description",locale)?if_exists}</option>
                    <option value="${adjReturnType.returnTypeId}">--</option>
                  </#if>
                  <#list returnTypes as returnTypeItem>
                    <option value="${returnTypeItem.returnTypeId}">${returnTypeItem.get("description",locale)?if_exists}</option>
                  </#list>
                </select>
          </#if>
          </div>
       </td>
       <#if (adjEditable)>
         <td align="right">
           <@submitFormLinkConfirm form="removeReturnAdjustmentAction" text=uiLabelMap.CommonRemove returnAdjustmentId=returnAdjustment.returnAdjustmentId />
         </td>
       <#else>
         <td>&nbsp;</td>
       </#if>
       <#assign rowCount = rowCount + 1>
       <#assign returnTotal = returnTotal + returnAdjustment.amount?default(0)>
    </tr>    
</#macro>

<@frameSectionHeader title=uiLabelMap.OrderItemsReturned />
<div class="form">
<table width="100%" border="0" cellpadding="2" cellspacing="0">
  <#assign readOnly = (returnHeader.statusId != "RETURN_REQUESTED")>
    
  <tr><td colspan="8"><hr class="sepbar"></td></tr>
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
    <td align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
    <td align="right"><div class="tableheadtext">${uiLabelMap.OrderPrice}</div></td>
    <td align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
    <td style="padding-left: 15px;"><div class="tableheadtext">${uiLabelMap.OrderReturnReason}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonProcessing}</div></td>
    <#if (readOnly)>
    <td><div class="tableheadtext">${uiLabelMap.OrderReturnResponse}</div></td>    
    </#if>    
    <td>&nbsp;</td>
  </tr>
  <tr><td colspan="8"><hr class="sepbar"></td></tr>
  <#assign returnTotal = 0.0>
  <#assign rowCount = 0>
  <@form name="removeReturnItemAction" url="removeReturnItem" returnId=returnId returnItemSeqId="" />
  <@form name="removeReturnAdjustmentAction" url="removeReturnAdjustment" returnId=returnId returnAdjustmentId="" />
  <form method="post" action="<@ofbizUrl>updateReturnItems</@ofbizUrl>">
  <input type="hidden" name="_useRowSubmit" value="Y">      
  <#if returnItems?has_content>
    <#list returnItems as item>
      <#assign orderItem = item.getRelatedOne("OrderItem")?if_exists>
      <#assign orderHeader = item.getRelatedOne("OrderHeader")?if_exists>
      <#assign returnReason = item.getRelatedOne("ReturnReason")?if_exists>
      <#assign returnType = item.getRelatedOne("ReturnType")?if_exists>
      <#if (item.get("returnQuantity")?exists && item.get("returnPrice")?exists)>
         <#assign returnTotal = returnTotal + item.get("returnQuantity") * item.get("returnPrice") >
         <#assign returnItemSubTotal = item.get("returnQuantity") * item.get("returnPrice") >
      <#else>
         <#assign returnItemSubTotal = null >  <#-- otherwise the last item's might carry over -->
      </#if>

      <tr>
          <input name="orderId_o_${rowCount}" value="${item.orderId}" type="hidden">
          <input name="returnId_o_${rowCount}" value="${item.returnId}" type="hidden">
          <input name="returnItemTypeId_o_${rowCount}" value="${item.returnItemTypeId}" type="hidden">
          <input name="returnItemSeqId_o_${rowCount}" value="${item.returnItemSeqId}" type="hidden">
          <input type="hidden" name="_rowSubmit_o_${rowCount}" value="Y" />
        <td><div class="tabletext">
            <#if item.get("productId")?exists>
                ${item.productId}
            <#else>
                N/A
            </#if></div></td>
        <td><div class="tabletext">
            <#if readOnly>
                ${item.description?default("N/A")}            
            <#else>
                <input name="description_o_${rowCount}" value="${item.description}" type="text" class='inputBox' size="30">
            </#if>
            </div></td>
        <td><div class="tabletextright">
            <#if readOnly>
                ${item.returnQuantity?string.number}
            <#else>
                <input name="returnQuantity_o_${rowCount}" value="${item.returnQuantity}" type="text" class='inputBox' size="4" align="right">
            </#if>
            </div></td>
        <td><div class="tabletextright">
            <#if readOnly>
                <@ofbizCurrency amount=item.returnPrice isoCode=orderHeader.currencyUom/>
            <#else>
                <input name="returnPrice_o_${rowCount}" value="${item.returnPrice}" type="text" class='inputBox' size="8" align="right">
            </#if>
            </div></td>
        <td class="tabletextright">
            <#if returnItemSubTotal?exists><@ofbizCurrency amount=returnItemSubTotal isoCode=orderHeader.currencyUom/></#if>
        </td>
        <td style="padding-left: 15px;"><div class="tabletext">
            <#if readOnly>
                ${returnReason.get("description",locale)?default("N/A")}
            <#else>
                <select name="returnReasonId_o_${rowCount}"  class='selectBox'>
                    <#if (returnReason?has_content)>
                        <option value="${returnReason.returnReasonId}">${returnReason.get("description",locale)?if_exists}</option>
                        <option value="${returnReason.returnReasonId}">--</option>
                    </#if>
                    <#list returnReasons as returnReasonItem>
                        <option value="${returnReasonItem.returnReasonId}">${returnReasonItem.get("description",locale)?if_exists}</option>
                    </#list>
                </select>
            </#if>
            </div></td>
        <td><div class="tabletext">
            <#if (readOnly)>
                ${returnType.get("description",locale)?default("N/A")}
            <#else>
                <select name="returnTypeId_o_${rowCount}" class="selectBox">
                    <#if (returnType?has_content)>
                        <option value="${returnType.returnTypeId}">${returnType.get("description",locale)?if_exists}</option>
                        <option value="${returnType.returnTypeId}">--</option>
                    </#if>
                    <#list returnTypes as returnTypeItem>
                        <option value="${returnTypeItem.returnTypeId}">${returnTypeItem.get("description",locale)?if_exists}</option>
                    </#list>
                </select>
            </#if></div></td>
        <#if (readOnly)>
          <td>
          <#if (item.statusId == "RETURN_COMPLETED") || (item.statusId=="RETURN_MAN_REFUND")>
            <#assign itemResp = item.getRelatedOne("ReturnItemResponse")?if_exists>
            <#if itemResp?has_content>
              <#if itemResp.paymentId?has_content>
                <div class="tabletext"><#if item.statusId=="RETURN_MAN_REFUND"><span class="requiredField">${uiLabelMap.CrmOrderManualRefund}:<br/></span></#if>${uiLabelMap.AccountingPayment} #${itemResp.paymentId}</div>
              <#elseif itemResp.replacementOrderId?has_content>
                <div class="tabletext">${uiLabelMap.OrderOrder} #<a href="<@ofbizUrl>orderview?orderId=${itemResp.replacementOrderId}</@ofbizUrl>" class="buttontext">${itemResp.replacementOrderId}</a></div>
              <#elseif itemResp.billingAccountId?has_content>
                <div class="tabletext">${uiLabelMap.AccountingAccountId} #${itemResp.billingAccountId}</div>
              </#if>
            <#else>
              <div class="tabletext">${uiLabelMap.CommonNone}</div>
            </#if>
          <#else>
            <div class="tabletext">${uiLabelMap.CommonNA}</div>
          </#if>
        </td>                  
        </#if>
        <#if returnHeader.statusId == "RETURN_REQUESTED">
          <td align='right'>
            <@submitFormLinkConfirm form="removeReturnItemAction" text=uiLabelMap.CommonRemove returnItemSeqId=item.returnItemSeqId />
          </td>
        <#else>
          <td>&nbsp;</td>
        </#if>
      </tr>
      <#assign rowCount = rowCount + 1>
      <#assign returnItemAdjustments = item.getRelated("ReturnAdjustment")>
      <#if (returnItemAdjustments?has_content)>
          <#list returnItemAdjustments as returnItemAdjustment>
             <@displayReturnAdjustment returnAdjustment=returnItemAdjustment adjEditable=false/>  <#-- adjustments of return items should never be editable -->
          </#list>
      </#if>
    </#list>
<#else>
    <tr>
      <td colspan="8"><div class="tabletext">${uiLabelMap.OrderNoReturnItemsFound}</div></td>
    </tr>
  </#if>
   <tr><td colspan="8"><hr class="sepbar"></td></tr>

<#-- these are general return adjustments not associated with a particular item (itemSeqId = "_NA_") -->
<#if (returnAdjustments?has_content)>                  
    <#list returnAdjustments as returnAdjustment>
        <#assign adjEditable = !readOnly/> <#-- they are editable if the rest of the return items are -->
        <@displayReturnAdjustment returnAdjustment=returnAdjustment adjEditable=adjEditable/>
    </#list>
    </#if>
    <#-- show the return total -->    
    <tr><td colspan="4"></td><td><hr class="sepbar"/></td></tr>
    <tr>
      <td>&nbsp;</td>
      <td colspan="3" class="tableheadtext">${uiLabelMap.OrderReturnTotal}</td>
      <td class="tabletextright"><b><@ofbizCurrency amount=returnTotal isoCode=returnHeader.currencyUomId/></b></td>
    </tr>
    <#if (!readOnly) && (rowCount > 0)>
       <tr>
          <input name="returnId" value="${returnHeader.returnId}" type="hidden">
          <input name="_rowCount" value="${rowCount}" type="hidden">
          <td colspan="8" class="tabletext" align="right"><input type="submit" class="smallSubmit" value="${uiLabelMap.OrderUpdateItems}"></td>
      </tr>
   </#if>
</form>

</table>
</div>
