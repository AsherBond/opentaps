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

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<#if orderHeader?has_content>

<#macro sectionSepBar>
   <tr><td colspan="4">&nbsp;</td><td colspan="2"><hr class="sepbar"/></td></tr>
</#macro>

<#assign newOrderLink = "<a href='loadCartFromOrder?${paramString}&finalizeMode=init' class='subMenuButton'>${uiLabelMap.OrderCreateAsNewOrder}</a>" />
<#if orderHeader.statusId != "ORDER_COMPLETED">
  <#assign cancelAllLink = "<a href='cancelOrderItem?${paramString}' class='subMenuButton'>${uiLabelMap.OrderCancelAllItems}</a>" />
  <#assign editItemsLink = "<a href='editOrderItems?${paramString}' class='subMenuButton'>${uiLabelMap.OpentapsOrderEditAddItem}</a>" />
  <#assign editDeliveryDatesLink = "<a href='editProductDeliveryDate?${paramString}' class='subMenuButton'>${uiLabelMap.PurchEstimatedDeliveryDate}</a>" />
</#if>
<#-- vendor returns are not supported right now
<#if returnableItems?has_content>
  <#assign createReturnLink = "<a href='/ordermgr/control/quickreturn?orderId=${orderId}&amp;party_id=${partyId?if_exists}&amp;returnHeaderTypeId=${returnHeaderTypeId}&${externalKeyParam}' class='subMenuButton'>${uiLabelMap.OrderCreateReturn}</a>" />
</#if>
 -->

<div class="screenlet">
    <div class="subSectionHeader">
        <div class="subSectionTitle">${uiLabelMap.OrderOrderItems}</div>
        <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && orderHeader?has_content>
          <div class="subMenuBar">
          <#if (orderHeader.statusId != "ORDER_CANCELLED") && (orderHeader.statusId != "ORDER_REJECTED")>
          ${editDeliveryDatesLink?if_exists}${editItemsLink?if_exists}${createReturnLink?if_exists}</#if>
          ${newOrderLink?if_exists}
          </div>
        </#if>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0" cellspacing="0">
          <tr align="left" valign=bottom>
            <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
            <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
            <td width="5%" align="center"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderUnitPrice}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderAdjustments}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
            <td width="5%">&nbsp;</td>
          </tr>
          <#if !orderItemList?has_content>
            <tr><td><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
          <#else>
            <#list orderItemList as orderItem>
              <#assign orderItemContentWrapper = Static["org.ofbiz.order.order.OrderContentWrapper"].makeOrderContentWrapper(orderItem, request)>
              <#assign orderItemShipGrpInvResList = orderReadHelper.getOrderItemShipGrpInvResList(orderItem)>
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                <#assign productId = orderItem.productId?if_exists>
                <#if productId?exists && productId == "shoppingcart.CommentLine">
                  <td colspan="1" valign="top">
                    <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                  </td>
                <#else>
                  <td valign="top">
                    <div class="tabletext">
                      <i>${orderItemType.description?default("")}:</i><br/> 
                      <#if productId?exists>
                        ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription?if_exists}
                      <#elseif orderItemType?exists>
                        ${orderItem.itemDescription?if_exists}
                      <#else>
                        ${orderItem.itemDescription?if_exists}
                      </#if>
                    </div>
                    <#if orderItem.comments?has_content>
                    <div class="tabletext">
                       <i>${uiLabelMap.CommonComments}: ${orderItem.comments}</i>
                    </div>
                    </#if>
                    
                  </td>

                  <#-- now show status details per line item -->
                  <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
                  <td align="left" colspan="1" valign="top">
                    <div class="tabletext">${uiLabelMap.CommonCurrent}: ${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</div>
                    <#assign orderItemStatuses = orderReadHelper.getOrderItemStatuses(orderItem)>
                    <#list orderItemStatuses as orderItemStatus>
                      <#assign loopStatusItem = orderItemStatus.getRelatedOne("StatusItem")>
                      <div class="tabletext">
                        ${getLocalizedDate(orderItemStatus.statusDatetime)} : ${loopStatusItem.get("description",locale)?default(orderItemStatus.statusId)}
                      </div>
                    </#list>
                    <#assign returns = orderItem.getRelated("ReturnItem")?if_exists>
                    <#if returns?has_content>
                      <#list returns as returnItem>
                        <#assign returnHeader = returnItem.getRelatedOne("ReturnHeader")>
                        <#if returnHeader.statusId != "RETURN_CANCELLED">
                          <div class="tabletext">
                            <font color="red"><b>${uiLabelMap.OrderReturned}</b></font> #<a href="<@ofbizUrl>returnMain?returnId=${returnItem.returnId}</@ofbizUrl>" class="buttontext">${returnItem.returnId}</a>
                          </div>
                        </#if>
                      </#list>
                    </#if>
                  </td>

                  <#-- QUANTITY -->
                  <td align="right" valign="top" nowrap="nowrap">
                    <table>
                      <tr valign="top">
                        <td>
                        <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                        <#assign shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)>
                        <#assign invoicedQuantity = orderReadHelper.getOrderItemInvoicedQuantity(orderItem)>
                        <#assign outstandingQuantity = remainingQuantity - invoicedQuantity?default(0)>
                          <div class="tabletext">${uiLabelMap.OrderOrdered}:&nbsp;${orderItem.quantity?default(0)?string.number}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderCancelled}:&nbsp;${orderItem.cancelQuantity?default(0)?string.number}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OpentapsOrderNetOrdered}:&nbsp;${remainingQuantity}&nbsp;&nbsp;</div>
                        </td>
                        <td>
                        <#if orderItem.orderItemTypeId == "PRODUCT_ORDER_ITEM">
                          <div class="tabletext">${uiLabelMap.CommonReceived}:&nbsp;${shippedQuantity}&nbsp;&nbsp;</div>
                        </#if>
                          <div class="tabletext">${uiLabelMap.OrderOutstanding}:&nbsp;${outstandingQuantity}
                          <div class="tabletext">${uiLabelMap.OrderInvoiced}:&nbsp;${orderReadHelper.getOrderItemInvoicedQuantity(orderItem)}&nbsp;&nbsp;</div>
                        </td>
                      </tr>
                    </table>
                  </td>

                  <td align="right" valign="top" nowrap="nowrap">
                    <div class="tabletext"><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></div>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false) isoCode=currencyUomId/></div>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <#if orderItem.statusId != "ITEM_CANCELLED">
                      <div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/></div>
                    <#else>
                      <div class="tabletext"><@ofbizCurrency amount=0.00 isoCode=currencyUomId/></div>
                    </#if>
                  </td>
                  <td>&nbsp;</td>
                  <td align="right" valign="top">
                    &nbsp;
                  </td>
                </#if>
              </tr>

              <#-- accounting tags -->
              <#if tagTypes?has_content>
                <tr>
                  <td colspan="2" valign="top">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%">
                      <@accountingTagsDisplayRows tags=tagTypes entity=orderItem/>
                    </table>
                  </td>
                </tr>
                <tr><td colspan="2">&nbsp;</td></tr>
              </#if>

              <#if (orderItem.orderItemTypeId="PRODUCT_ORDER_ITEM") && (productId?exists)>
              <#-- INVENTORY -->
                <#if (orderHeader.statusId != "ORDER_COMPLETED") && availableToPromiseMap?exists && quantityOnHandMap?exists && availableToPromiseMap.get(productId)?exists && quantityOnHandMap.get(productId)?exists>
                <tr><td colspan="3"/>
                  <#assign quantityToProduce = 0>
                  <#assign product = orderItem.getRelatedOneCache("Product")>
                  <#if product.productTypeId != "DIGITAL_GOOD">
                    <#assign atpQuantity = availableToPromiseMap.get(productId)?default(0)>
                    <#assign qohQuantity = quantityOnHandMap.get(productId)?default(0)>
                    <#assign mktgPkgATP = mktgPkgATPMap.get(productId)?default(0)>
                    <#assign mktgPkgQOH = mktgPkgQOHMap.get(productId)?default(0)>
                    <#assign requiredQuantity = requiredProductQuantityMap.get(productId)?default(0)>
                    <#assign onOrderQuantity = onOrderProductQuantityMap.get(productId)?default(0)>
                    <#assign inProductionQuantity = productionProductQuantityMap.get(productId)?default(0)>
                    <#assign unplannedQuantity = requiredQuantity - qohQuantity - inProductionQuantity - onOrderQuantity - mktgPkgQOH>
                    <#if unplannedQuantity < 0><#assign unplannedQuantity = 0></#if>
                    <div class="tabletext" style="margin-top: 8px; margin-left: 20px;">
                      <table cellspacing="0" cellpadding="0" border="0">
                        <tr>
                          <td align="left">${uiLabelMap.OrderRequiredForSO}</td>
                          <td style="padding-left: 15px; text-align: left;">${requiredQuantity}</td>                     
                        </tr>
                        <tr>
                          <td align="left">${uiLabelMap.ProductInInventory} ${uiLabelMap.ProductQoh}</td>
                          <td style="padding-left: 15px; text-align: left;">${qohQuantity} (${uiLabelMap.ProductAtp}: ${atpQuantity})</td>
                        </tr>
                        <#if (product != null) && (product.productTypeId != null) && (product.productTypeId == "MARKETING_PKG_AUTO")>
                        <tr>
                          <td align="left">${uiLabelMap.ProductMarketingPackageQOH}</td>
                          <td style="padding-left: 15px; text-align: left;">${mktgPkgQOH} (${uiLabelMap.ProductAtp}: ${mktgPkgATP})</td>
                        </tr>
                        </#if>
                        <tr>
                          <td align="left">${uiLabelMap.OrderOnOrder}</td>
                          <td style="padding-left: 15px; text-align: left;">${onOrderQuantity}</td>
                        </tr>
                        <tr>
                          <td align="left">${uiLabelMap.OrderInProduction}</td>
                          <td style="padding-left: 15px; text-align: left;">${inProductionQuantity}</td>
                        </tr>
                        <tr>
                          <td align="left">${uiLabelMap.OrderUnplanned}</td>
                          <td style="padding-left: 15px; text-align: left;">${unplannedQuantity}</td>
                        </tr>
                      </table>
                    </div>
                  </#if>
                </#if>
                </td></tr>
              </#if>

              <#-- show info from workeffort -->
              <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
              <#if workOrderItemFulfillments?has_content>
                  <#list workOrderItemFulfillments as workOrderItemFulfillment>
                      <#assign workEffort = workOrderItemFulfillment.getRelatedOneCache("WorkEffort")>
                      <tr>
                        <td>&nbsp;</td>
                        <td colspan="9">
                          <div class="tabletext">
                            <#if orderItem.orderItemTypeId != "RENTAL_ORDER_ITEM">
                              ${uiLabelMap.PurchTask}: ${workEffort.workEffortId}&nbsp;
                            </#if>
                          </div>
                        </td>
                      </tr>
                      <#break><#-- need only the first one -->
                  </#list>
              </#if>
              <#-- show linked order lines -->
              <#assign linkedOrderItemsTo = delegator.findByAnd("OrderItemAssoc", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId", orderItem.getString("orderId"),
                                                                                                                               "orderItemSeqId", orderItem.getString("orderItemSeqId")))>
              <#assign linkedOrderItemsFrom = delegator.findByAnd("OrderItemAssoc", Static["org.ofbiz.base.util.UtilMisc"].toMap("toOrderId", orderItem.getString("orderId"),
                                                                                                                                 "toOrderItemSeqId", orderItem.getString("orderItemSeqId")))>
              <#if linkedOrderItemsTo?has_content>
                <#list linkedOrderItemsTo as linkedOrderItem>
                  <#assign linkedOrderId = linkedOrderItem.toOrderId>
                  <#assign linkedOrderItemSeqId = linkedOrderItem.toOrderItemSeqId>
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        ${uiLabelMap.OrderLinkedToOrderItem} : ${linkedOrderId} / ${linkedOrderItemSeqId}&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>
              <#if linkedOrderItemsFrom?has_content>
                <#list linkedOrderItemsFrom as linkedOrderItem>
                  <#assign linkedOrderId = linkedOrderItem.orderId>
                  <#assign linkedOrderItemSeqId = linkedOrderItem.orderItemSeqId>
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        ${uiLabelMap.OrderLinkedFromOrderItem} : ${linkedOrderId} / ${linkedOrderItemSeqId}&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>
              <#-- show linked requirements -->
              <#assign linkedRequirements = orderItem.getRelated("OrderRequirementCommitment")?if_exists>

              <#if linkedRequirements?has_content>
                <#list linkedRequirements as linkedRequirement>
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        <b><i>${uiLabelMap.OrderLinkedToRequirement}</i>:</b>
                        <a href="<@ofbizUrl>EditRequirement?requirementId=${linkedRequirement.requirementId}</@ofbizUrl>" class="buttontext" style="font-size: xx-small;">${linkedRequirement.requirementId}</a>&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- show linked quote -->
              <#assign linkedQuote = orderItem.getRelatedOneCache("QuoteItem")?if_exists>

              <#if linkedQuote?has_content>
                <tr>
                  <td>&nbsp;</td>
                  <td colspan="9">
                    <div class="tabletext">
                      ${uiLabelMap.OrderLinkedToQuote}: ${linkedQuote.quoteId}-${linkedQuote.quoteItemSeqId}&nbsp;
                    </div>
                  </td>
                </tr>
              </#if>

              <#-- now show adjustment details per line item -->
              <#assign orderItemAdjustments = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentList(orderItem, orderAdjustments)>
              <#if orderItemAdjustments?exists && orderItemAdjustments?has_content>
                <#list orderItemAdjustments as orderItemAdjustment>
                  <#assign adjustmentType = orderItemAdjustment.getRelatedOneCache("OrderAdjustmentType")>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.OrderAdjustment}</i>:</b> <b>${adjustmentType.get("description",locale)}</b>:
                        ${orderItemAdjustment.get("description",locale)?if_exists} 
                        <#if orderItemAdjustment.comments?has_content>(${orderItemAdjustment.comments?default("")})</#if>
                        <#if orderItemAdjustment.productPromoId?has_content><a href="/catalog/control/EditProductPromo?productPromoId=${orderItemAdjustment.productPromoId}&amp;externalLoginKey=${externalLoginKey}">${orderItemAdjustment.getRelatedOne("ProductPromo").getString("promoName")}</a></#if>
                        <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                          <#if orderItemAdjustment.primaryGeoId?has_content>
                            <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
	                        <#if primaryGeo.geoName?has_content>
	                            <b>${uiLabelMap.OrderJurisdiction}:</b> ${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
	                        </#if>
                            <#if orderItemAdjustment.secondaryGeoId?has_content>
                              <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                              (<b>${uiLabelMap.CommonIn}:</b> ${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                            </#if>
                          </#if>
                          <#if orderItemAdjustment.sourcePercentage?exists><b>${uiLabelMap.OrderRate}:</b> ${orderItemAdjustment.sourcePercentage?string("0.######")}</#if>
                          <#if orderItemAdjustment.customerReferenceId?has_content><b>${uiLabelMap.OrderCustomerTaxId}:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                          <#if orderItemAdjustment.exemptAmount?exists><b>${uiLabelMap.OrderExemptAmount}:</b> ${orderItemAdjustment.exemptAmount}</#if>
                        </#if>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right">
                      <div class="tabletext" style="font-size: xx-small;">
                        <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcItemAdjustment(orderItemAdjustment, orderItem) isoCode=currencyUomId/>
                      </div>
                     </td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show survey information per line item -->
              <#assign orderItemSurveyResponses = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSurveyResponse(orderItem)>
              <#if orderItemSurveyResponses?exists && orderItemSurveyResponses?has_content>
                <#list orderItemSurveyResponses as survey>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.CommonSurveys}</i>:</b>
                          <a href="/content/control/ViewSurveyResponses?surveyResponseId=${survey.surveyResponseId}&amp;surveyId=${survey.surveyId}<#if survey.partyId?exists>&amp;partyId=${survey.partyId}</#if>&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${survey.surveyId}</a>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <!-- display the ship before/after dates -->
              <#if orderItem.shipAfterDate?exists>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipAfterDate}</i>:</b> ${getLocalizedDate(orderItem.shipAfterDate, "DATE")}</div>
                </td>
              </tr>
              </#if>
              <#if orderItem.shipBeforeDate?exists>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipBeforeDate}</i>:</b> ${getLocalizedDate(orderItem.shipBeforeDate, "DATE")}</div>
                </td>
              </tr>
              </#if>

              <#-- now show ship group info per line item -->
              <#if orderItem.orderItemTypeId="PRODUCT_ORDER_ITEM">              
              <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc")?if_exists>
              <#if orderItemShipGroupAssocs?has_content>
                <#list orderItemShipGroupAssocs as shipGroupAssoc>
                  <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")>
                  <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${shipGroupAssoc.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right" valign="top">
                      &nbsp;
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- now show planned shipment info per line item -->
              <#assign orderShipments = orderItem.getRelated("OrderShipment")?if_exists>
              <#if orderShipments?has_content>
                <#list orderShipments as orderShipment>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderPlannedInShipment}</i>: </b><a target="facility" href="/warehouse/control/ViewShipment?shipmentId=${orderShipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${orderShipment.shipmentId}</a>: ${orderShipment.shipmentItemSeqId}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${orderShipment.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              </#if>
            </#list>
          </#if>

          <#-- subtotal -->
          <@sectionSepBar />
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderItemsSubTotal}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></div></td>
          </tr>
          <#-- tax adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalSalesTax}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></div></td>
          </tr>


          <#macro displayAdjustment adjustment>
            <#assign adjustmentType = adjustment.getRelatedOneCache("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(adjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
              <tr>
                <td align="right" colspan="4">
                  <div class="tabletext"><b>${adjustmentType.get("description",locale)}</b> ${adjustment.comments?if_exists}  ${adjustment.get("description")?if_exists} : </div>
                </td>
                <td align="right" nowrap="nowrap">
                  <div class="tabletext"><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
              </tr>
            </#if>
          </#macro>

          <#-- shipping adjustments -->
          <tr><td colspan="8"><hr class="sepbar"/></td></tr>
          <#list shippingAdjustments?default([]) as shippingAdjustment>
              <@displayAdjustment adjustment=shippingAdjustment/>
          </#list>
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalShippingAndHandling}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></div></td>
          </tr>

          <#-- other adjustments -->
          <tr><td colspan="8"><hr class="sepbar"/></td></tr>
          <#list otherAdjustments?default([]) as otherAdjustment>
              <@displayAdjustment adjustment=otherAdjustment/>
          </#list>
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalOtherOrderAdjustments}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></div></td>
          </tr>
          <@sectionSepBar/>
            
          <#-- grand total -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalDue}</b></div></td>
            <td align="right" nowrap="nowrap">
              <div class="tabletext"><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></div>
            </td>
          </tr>
        </table>
    </div>
</div>

</#if>
