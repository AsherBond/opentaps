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
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsSurveyMacros.ftl"/>

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#if maySelectItems?default(false)>
                <a href="javascript:document.addOrderToCartForm.add_all.value="true";document.addOrderToCartForm.submit()" class="lightbuttontext">${uiLabelMap.OrderAddAllToCart}</a>
                <a href="javascript:document.addOrderToCartForm.add_all.value="false";document.addOrderToCartForm.submit()" class="lightbuttontext">${uiLabelMap.OrderAddCheckedToCart}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
          <tr align="left" valign="bottom">
            <td width="65%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductProduct}</b></span></td>
            <td width="5%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQuantity}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonUnitPrice}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderSubTotal}</b></span></td>
          </tr>
          <#list orderItems?if_exists as orderItem>
            <#assign itemType = orderItem.getRelatedOne("OrderItemType")>
            <tr><td colspan="6"><hr class="sepbar"/></td></tr>
            <#assign warnings = []>
            <tr>     
              <#if orderItem.productId?exists && orderItem.productId == "_?_">           
                <td colspan="1" valign="top">    
                  <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                </td>
              <#else>                  
                <td valign="top">                      
                  <div class="tabletext"> 
                    <#if orderItem.productId?exists>
                      <#assign warnings = cart.getProductWarnings(delegator, orderItem.productId)>
                      <a href="<@ofbizUrl>product?product_id=${orderItem.productId}</@ofbizUrl>" class="linktext">${orderItem.productId}: ${orderItem.itemDescription}</a>
                    <#else>                                                    
                      <b>${itemType.description}</b> : ${orderItem.itemDescription?if_exists}
                    </#if>
                  </div>
                  
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap="nowrap">${orderItem.quantity?string.number}</div>
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap="nowrap"><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></div>
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap="nowrap"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentsTotal(orderItem) isoCode=currencyUomId rounding=2/></div>
                </td>
                <td align="right" valign="top" nowrap="nowrap">
                  <div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemSubTotal(orderItem) isoCode=currencyUomId/></div>
                </td>                    
                <#if maySelectItems?default(false)>
                  <td>                                 
                    <input name="item_id" value="${orderItem.orderItemSeqId}" type="checkbox">
                  </td>
                </#if>
              </#if>
            </tr>
            <#if warnings?has_content>
	      <#list warnings as warning>
	        <tr><td class="productWarning" colspan="6">${uiLabelMap.CrmProductWarning} : ${warning?if_exists}</td></tr>
	      </#list>  
            </#if>
            <#-- print the survey based on a macro defined externally (see top of file @imports) -->
            <#assign cartLine = cart.findCartItem(orderItem.orderItemSeqId) >
            <#if cartLine.getAttribute("surveyResponses")?has_content>
              <tr style="margin-left: 10px;">
                <td>
                  <#list cartLine.getAttribute("surveyResponses") as surveyResponseId>
                    <@displaySurveyResponse surveyResponseId=surveyResponseId/>
                  </#list>
                </td>
              </tr>
            </#if>
            <#-- show info from workeffort if it was a rental item -->
            <#if orderItem.orderItemTypeId == "RENTAL_ORDER_ITEM">
                <#assign WorkOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
                <#if WorkOrderItemFulfillments?has_content>
                    <#list WorkOrderItemFulfillments as WorkOrderItemFulfillment>
                        <#assign workEffort = WorkOrderItemFulfillment.getRelatedOneCache("WorkEffort")?if_exists>
                          <tr><td>&nbsp;</td><td>&nbsp;</td><td colspan="8"><div class="tabletext">${uiLabelMap.CommonFrom}: ${getLocalizedDate(workEffort.estimatedStartDate, "DATE")} ${uiLabelMap.CommonTo}: ${getLocalizedDate(workEffort.estimatedCompletionDate, "DATE")} ${uiLabelMap.OrderNbrPersons}: ${workEffort.reservPersons}</div></td></tr>
                        <#break><#-- need only the first one -->
                    </#list>
                </#if>
            </#if>

            <#-- now show adjustment details per line item -->
            <#assign itemAdjustments = localOrderReadHelper.getOrderItemAdjustments(orderItem)>
            <#list itemAdjustments as orderItemAdjustment>
              <tr>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;">
                    <b><i>${uiLabelMap.OrderAdjustment}</i>:</b> <b>${localOrderReadHelper.getAdjustmentType(orderItemAdjustment)}</b>&nbsp;
                    <#if orderItemAdjustment.description?has_content>: ${orderItemAdjustment.get("description",locale)}</#if>

                    <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                      <#if orderItemAdjustment.primaryGeoId?has_content>
                        <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
                        <#if primaryGeo.geoName?has_content>
                            <b>${uiLabelMap.OrderJurisdiction}:</b> ${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
                        </#if>
                        <#if orderItemAdjustment.secondaryGeoId?has_content>
                          <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                          (<b>in:</b> ${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                        </#if>
                      </#if>
                      <#if orderItemAdjustment.sourcePercentage?exists><b>${uiLabelMap.OrderRate}:</b> ${orderItemAdjustment.sourcePercentage}%</#if>
                      <#if orderItemAdjustment.customerReferenceId?has_content><b>${uiLabelMap.OrderCustomerTaxId}:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                      <#if orderItemAdjustment.exemptAmount?exists><b>${uiLabelMap.OrderExemptAmount}:</b> ${orderItemAdjustment.exemptAmount}</#if>
                    </#if>
                  </div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentTotal(orderItem, orderItemAdjustment) isoCode=currencyUomId rounding=2/></div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <#if maySelectItems?default(false)><td>&nbsp;</td></#if>
              </tr>
            </#list>

              <#-- accounting tags -->
              <tr>
                <td colspan="4" valign="top">
                  <table border="0" cellpadding="0" cellspacing="0" width="100%">
                    <#if tagTypes?has_content>
                      <@accountingTagsDisplayForCart tags=tagTypes item=cartLine/>
                    </#if>
                  </table>
                </td>
              </tr>
           </#list>
           <#if !orderItems?has_content>
             <tr><td><font color="red">${uiLabelMap.checkhelpertotalsdonotmatchordertotal}</font></td></tr>
           </#if>

          <tr><td colspan="8"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderSubTotal}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext">&nbsp;<#if orderSubTotal?exists><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></#if></div></td>
          </tr>              
          <#list headerAdjustmentsToShow?if_exists as orderHeaderAdjustment>
            <tr>
              <td align="right" colspan="4"><div class="tabletext"><b>${localOrderReadHelper.getAdjustmentType(orderHeaderAdjustment)}</b></div></td>
              <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderAdjustmentTotal(orderHeaderAdjustment) isoCode=currencyUomId rounding=2/></div></td>
            </tr>
          </#list>                 

          <tr><td colspan="2"></td><td colspan="8"><hr class="sepbar"/></td></tr>
          <#if shippingAdjustments?has_content>
          <#list shippingAdjustments as shippingAdjustment>
            <#assign adjustmentType = shippingAdjustment.getRelatedOneCache("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(shippingAdjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
              <tr>
                <td align="right" colspan="3">
                  <div class="tabletext"><b>${adjustmentType.get("description",locale)}</b> <#if shippingAdjustment.description?has_content>(${shippingAdjustment.description})</#if>: </div>
                </td>
                <td align="right" nowrap="nowrap">
                  <div class="tabletext"><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId rounding=2/></div>
                </td>
                <td>&nbsp;</td>
              </tr>              
            </#if>
          </#list>
          </#if>
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderTotalShippingAndHandling}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=orderShippingTotal isoCode=currencyUomId/></div></td>
          </tr>
          <tr><td colspan="2"></td><td colspan="8"><hr class="sepbar"/></td></tr>

          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderSalesTax}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><#if orderTaxTotal?exists><@ofbizCurrency amount=orderTaxTotal isoCode=currencyUomId/></#if></div></td>
          </tr>
          
          <tr><td colspan="2"></td><td colspan="8"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderGrandTotal}</b></div></td>
            <td align="right" nowrap="nowrap">
              <div class="tabletext"><#if orderGrandTotal?exists><@ofbizCurrency amount=orderGrandTotal isoCode=currencyUomId/></#if></div>
            </td>
          </tr>
        </table>
    </div>
</div>
