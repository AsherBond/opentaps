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
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsSurveyMacros.ftl"/>

<script type="text/javascript">
    var reReservationWidgetParameters = new Object();
</script>

<#function makeName name itemIndex=-1 reservIndex=-1>
  <#if itemIndex == -1>
    <#return name>
  <#else>
    <#return name + "_" + itemIndex + "_" + reservIndex>
  </#if>
</#function>

<#if order?exists>

<#macro sectionSepBar>
   <tr><td colspan="4">&nbsp;</td><td colspan="2"><hr class="sepbar"/></td></tr>
</#macro>

<@form name="createReturnAction" url="createReturnFromOrder" orderId=order.orderId />

<#assign newOrderLink><a href="<@ofbizUrl>loadCartFromOrder?${paramString}&finalizeMode=init</@ofbizUrl>" class="subMenuButton">${uiLabelMap.OrderCreateAsNewOrder}</a></#assign>
<#if !order.isCompleted()>
  <#assign cancelAllLink><a href="<@ofbizUrl>cancelOrderItem?${paramString}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.OrderCancelAllItems}</a></#assign>
  <#assign editItemsLink><a href="<@ofbizUrl>editOrderItems?${paramString}</@ofbizUrl>" class="subMenuButton">${uiLabelMap.OpentapsOrderEditAddItem}</a></#assign>
</#if>
<#if order.returnableItemsMap?has_content && security.hasEntityPermission("CRMSFA", "_RETURN_CREATE", userLogin)>
  <#assign createReturnLink><@submitFormLink form="createReturnAction" class="subMenuButton" text=uiLabelMap.OrderCreateReturn/></#assign>
</#if>

<#assign itemLinks>
  <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
    <#if !order.isCancelled() && !order.isRejected()>
      ${editItemsLink?if_exists}${createReturnLink?if_exists}
    </#if>
    ${newOrderLink?if_exists}
  </#if>
</#assign>

<@flexAreaClassic targetId="orderItems" title=uiLabelMap.OrderOrderItems defaultState="open" headerContent=itemLinks style="margin:0">
  <table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr align="left" valign="bottom">
      <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
      <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
      <td width="5%" align="center"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
      <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderUnitList}</div></td>
      <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderAdjustments}</div></td>
      <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
      <td width="5%">&nbsp;</td>
    </tr>
    <#if !order.items?has_content>
      <tr><td colspan="8"><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
    <#else>
      <#list order.items as item>
      <script type="text/javascript">
        reReservationWidgetParameters["orderItemSeqId_${item_index}"] = "${item.orderItemSeqId}";
      </script>
       <#if !(item.isPromo() && item.isCancelled())>
        <#assign orderItemGV = Static["org.opentaps.foundation.repository.ofbiz.Repository"].genericValueFromEntity(delegator, item)/>
        <#assign orderItemContentWrapper = Static["org.ofbiz.order.order.OrderContentWrapper"].makeOrderContentWrapper(orderItemGV, request)/>
        <tr><td colspan="8"><hr class="sepbar"/></td></tr>
        <#assign warnings = []/>
        <tr>
          <#if item.productId?exists && item.productId == "shoppingcart.CommentLine">
            <td colspan="1" valign="top">
              <b><div class="tabletext"> &gt;&gt; ${item.itemDescription}</div></b>
              <#if item.shoppingListId?exists>
              ${uiLabelMap.CrmPurchasedFromShoppingList} <a href="<@ofbizUrl>/viewShoppingList?shoppingListId=${item.shoppingListId}</@ofbizUrl>" class="linktext">${item.shoppingListId}</a>
              </#if>              
            </td>
          <#else>
            <td valign="top">
              <div class="tabletext">
                <#if item.productId?exists>
                  <#assign warnings = Static["org.opentaps.common.product.UtilProduct"].getProductWarnings(delegator, item.productId)/>
                  ${item.productId?default("N/A")} - ${item.itemDescription?if_exists}
                <#elseif item.type?exists>
                  ${item.type.description} - ${item.itemDescription?if_exists}
                <#else>
                  ${item.itemDescription?if_exists}
                </#if>
              </div>

              <#-- print the survey based on a macro defined externally (see top of file @imports) -->
              <#assign orderItemSurveyResponses = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSurveyResponse(orderItemGV)/>
              <#if orderItemSurveyResponses?exists && orderItemSurveyResponses?has_content>
                <#list orderItemSurveyResponses as survey>
                  <div class="tabletext" style="font-size: xx-small;">
                    <@displaySurveyResponse surveyResponseId=survey.surveyResponseId/>
                  </div>
                </#list>
              </#if>

              <#if item.comments?has_content>
              <div class="tabletext">
                 <i>${uiLabelMap.CommonComments}: ${item.comments}</i>
              </div>
              </#if>
              <#if item.shoppingListId?exists>
              ${uiLabelMap.CrmPurchasedFromShoppingList} <a href="<@ofbizUrl>/viewShoppingList?shoppingListId=${item.shoppingListId}</@ofbizUrl>" class="linktext">${item.shoppingListId}</a>
              </#if>              
            </td>

            <#-- now show status details per line item -->
            <td align="left" colspan="1" valign="top">
              <div class="tabletext">${uiLabelMap.CommonCurrent}: ${item.status.get("description",locale)?default(item.statusId)}</div>
              <#list item.orderStatuses as status>
                <div class="tabletext">
                  ${getLocalizedDate(status.statusDatetime)} : ${status.statusItem.get("description",locale)?default(status.statusId)}
                </div>
              </#list>
              <#list item.returnItems as returnItem>
                <#if !returnItem.return.isCancelled()>
                  <div class="tabletext">
                    <font color="red"><b>${uiLabelMap.OrderReturned}</b></font> #<a href="<@ofbizUrl>viewReturn?returnId=${returnItem.returnId}</@ofbizUrl>" class="linktext">${returnItem.returnId}</a>
                  </div>
                </#if>
              </#list>
            </td>

            <#-- QUANTITY -->
            <td align="right" valign="top" nowrap="nowrap">
              <table>
                <tr valign="top">
                  <td>
                    <div class="tabletext">${uiLabelMap.OrderOrdered}:&nbsp;${item.quantity}&nbsp;&nbsp;</div>
                    <div class="tabletext">${uiLabelMap.OrderCancelled}:&nbsp;${item.cancelQuantity}&nbsp;&nbsp;</div>
                    <div class="tabletext">${uiLabelMap.OpentapsOrderNetOrdered}:&nbsp;${item.orderedQuantity}&nbsp;&nbsp;</div>
                  </td>
                  <td>
                    <#if item.productId?exists && item.isPhysical()>
                      <div class="tabletext">${uiLabelMap.OrderShortfalled}:&nbsp;${item.shortfalledQuantity}&nbsp;&nbsp;</div>
                      <div class="tabletext">${uiLabelMap.ProductReserved}:&nbsp;${item.reservedQuantity}&nbsp;&nbsp;</div>
                      <div class="tabletext">${uiLabelMap.OrderQuantityShipped}:&nbsp;${item.shippedQuantity}&nbsp;&nbsp;</div>
                      <div class="tabletext">${uiLabelMap.OrderOutstanding}:&nbsp;${item.remainingToShipQuantity}&nbsp;&nbsp;</div>
                    </#if>
                    <div class="tabletext">${uiLabelMap.OrderInvoiced}:&nbsp;${item.invoicedQuantity}&nbsp;&nbsp;</div>
                    <div class="tabletext">${uiLabelMap.OrderReturned}:&nbsp;${item.returnedQuantity}&nbsp;&nbsp;</div>
                  </td>
                </tr>
              </table>
            </td>
            
            <td align="right" valign="top" nowrap="nowrap">
              <div class="tabletext"><@ofbizCurrency amount=item.unitPrice isoCode=order.currencyUom/><#if item.unitListPrice?has_content> / <@ofbizCurrency amount=item.unitListPrice isoCode=order.currencyUom/></#if></div>
            </td>
            <td align="right" valign="top" nowrap="nowrap">
              <div class="tabletext"><@ofbizCurrency amount=item.otherAdjustmentsAmount isoCode=order.currencyUom/></div>
            </td>
            <td align="right" valign="top" nowrap="nowrap">
              <#if !item.isCancelled()>
                <div class="tabletext"><@ofbizCurrency amount=item.subTotal isoCode=order.currencyUom/></div>
              <#else>
                <div class="tabletext"><@ofbizCurrency amount=0.00 isoCode=order.currencyUom/></div>
              </#if>
            </td>
            <td>&nbsp;</td>
            <td align="right" valign="top">
              &nbsp;
            </td>
          </#if>
        </tr>

        <#-- accounting tags -->
        <#if !item.isPromo() && tagTypes?has_content>
          <tr>
            <td colspan="2" valign="top">
              <table border="0" cellpadding="0" cellspacing="0" width="100%">
                <@accountingTagsDisplayRows tags=tagTypes entity=item/>
              </table>
            </td>
          </tr>
          <tr><td colspan="2">&nbsp;</td></tr>
        </#if>

        <#-- display product warnings -->
        <#if warnings?has_content>
            <#list warnings as warning>
                <tr><td class="productWarning" colspan="8">${uiLabelMap.CrmProductWarning} : ${warning?if_exists}</td></tr>
            </#list>
        </#if>

        <tr>
          <td style="padding-top : 10px; padding-bottom : 10px;" colspan="3">
            <#if item.productId?exists>
              <#-- INVENTORY -->
              <#if item.isPhysical() && (!order.isCompleted()) && availableToPromiseMap?exists && quantityOnHandMap?exists && availableToPromiseMap.get(item.productId)?exists && quantityOnHandMap.get(item.productId)?exists>
                <#assign quantityToProduce = 0/>
                <#assign atpQuantity = availableToPromiseMap.get(item.productId)?default(0)/>
                <#assign qohQuantity = quantityOnHandMap.get(item.productId)?default(0)/>
                <#assign mktgPkgATP = mktgPkgATPMap.get(item.productId)?default(0)/>
                <#assign mktgPkgQOH = mktgPkgQOHMap.get(item.productId)?default(0)/>
                <#assign requiredQuantity = requiredProductQuantityMap.get(item.productId)?default(0)/>
                <#assign onOrderQuantity = onOrderProductQuantityMap.get(item.productId)?default(0)/>
                <#assign inProductionQuantity = productionProductQuantityMap.get(item.productId)?default(0)/>
                <#assign unplannedQuantity = requiredQuantity - qohQuantity - inProductionQuantity - onOrderQuantity - mktgPkgQOH/>
                <#assign productDeliveryDates =  productDeliveryDateMap.get(item.productId) />
                <#if unplannedQuantity lt 0><#assign unplannedQuantity = 0/></#if>
                
                <div class="tabletext" style="margin-top: 15px; margin-left: 20px;">
                  <table cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td align="left">${uiLabelMap.OrderRequiredForSO}</td>
                      <td style="padding-left: 15px; text-align: left;">${requiredQuantity}</td>
                      <#if productDeliveryDates?has_content>
                        <td rowspan="5" valign="top">
                          <table cellspacing="0" cellpadding="0" border="0" style="margin-left:15px">
                            <tr><td colspan="3" align="center">${uiLabelMap.PurchDeliverySchedule}</td></tr>
                            <tr><td colspan="3"><hr class="sepbar"/></td></tr>
                            <#-- First 4 are displayed directly -->
                            <#list productDeliveryDates as delivery>
                              <tr>
                                <td>${delivery.orderId}</td>
                                <td style="padding-left:15px"><@displayDate date=delivery.estimatedReadyDate format="DATE" default=uiLabelMap.CommonNA /></td>
                                <td style="padding-left:15px">${delivery.quantity}</td>
                              </tr>
                              <#if delivery_index gte 3><#break/></#if>
                            </#list>
                            <#-- Other dates are displayed in a flex area -->
                            <#if productDeliveryDates?size gt 4>
                              <tr>
                                <td colspan="3">
                                  <@flexArea targetId="deliveryDates_${item.orderItemSeqId}" title="${uiLabelMap.PurchSeeAllDeliveryDates} (${productDeliveryDates?size-4})" style="border:0;margin:0;padding:5px 0 0 0" controlStyle="top:0;margin-left:0;border:0;" decoratorStyle="margin-left:0;margin-right:0">
                                    <table cellspacing="0" cellpadding="0" border="0" width="100%">
                                      <#list productDeliveryDates as delivery>
                                        <#if delivery_index gte 4>
                                          <tr>
                                            <td>${delivery.orderId}</td>
                                            <td style="padding-left:15px"><@displayDate date=delivery.estimatedReadyDate format="DATE" default=uiLabelMap.CommonNA /></td>
                                            <td style="padding-left:15px">${delivery.quantity}</td>
                                          </tr>
                                        </#if>
                                      </#list>
                                    </table>
                                  </@flexArea>
                                </td>
                              </tr>
                            </#if>
                          </table>
                        </td>
                      </#if>
                    </tr>
                    <tr>
                      <td align="left">${uiLabelMap.ProductInInventory} ${uiLabelMap.ProductQoh}</td>
                      <td style="padding-left: 15px; text-align: left;">${qohQuantity} (${uiLabelMap.ProductAtp}: ${atpQuantity})</td>
                    </tr>
                    <#if (item.product != null) && (item.product.productTypeId != null) && (item.product.productTypeId == "MARKETING_PKG_AUTO")>
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
          </td>
        </tr>

        <#-- show info from workeffort -->
        <#assign workOrderItemFulfillments = orderItemGV.getRelated("WorkOrderItemFulfillment")?if_exists/>
        <#if workOrderItemFulfillments?has_content>
          <#list workOrderItemFulfillments as workOrderItemFulfillment>
            <#assign workEffort = workOrderItemFulfillment.getRelatedOneCache("WorkEffort")/>
            <tr>
              <td>&nbsp;</td>
              <td colspan="9">
                <div class="tabletext">${uiLabelMap.CrmTask}: ${workEffort.workEffortId}&nbsp;
                </div>
              </td>
            </tr>
            <#break/><#-- need only the first one -->
          </#list>
        </#if>

        <#-- show linked order lines -->
        <#list item.itemAssocsTo as linkedOrderItem>
          <#assign linkedOrderId = linkedOrderItem.toOrderId />
          <#assign linkedOrderItemSeqId = linkedOrderItem.toOrderItemSeqId />
          <tr>
            <td>&nbsp;</td>
            <td colspan="9">
              <div class="tabletext">
                ${uiLabelMap.OrderLinkedToOrderItem}: ${linkedOrderId} / ${linkedOrderItemSeqId}&nbsp;
              </div>
            </td>
          </tr>
        </#list>
        <#list item.itemAssocsFrom as linkedOrderItem>
          <#assign linkedOrderId = linkedOrderItem.orderId />
          <#assign linkedOrderItemSeqId = linkedOrderItem.orderItemSeqId />
          <tr>
            <td>&nbsp;</td>
            <td colspan="9">
              <div class="tabletext">
                ${uiLabelMap.OrderLinkedFromOrderItem}: ${linkedOrderId} / ${linkedOrderItemSeqId}&nbsp;
              </div>
            </td>
          </tr>
        </#list>

        <#-- show linked requirements -->
        <#list item.requirementCommitments as linkedRequirement>
          <tr>
            <td>&nbsp;</td>
            <td colspan="9">
              <div class="tabletext">
                ${uiLabelMap.OrderLinkedToRequirement}: ${linkedRequirement.requirementId}&nbsp;
              </div>
            </td>
          </tr>
        </#list>

        <#-- show linked quote -->
        <#if item.quoteItem?has_content>
          <tr>
            <td>&nbsp;</td>
            <td colspan="9">
              <div class="tabletext">
                ${uiLabelMap.OrderLinkedToQuote}: ${item.quoteId} / ${item.quoteItemSeqId}&nbsp;
              </div>
            </td>
          </tr>
        </#if>

        <#-- now show adjustment details per line item -->
        <#list item.adjustments as orderItemAdjustment>
          <tr>
            <td align="right" colspan="2">
              <div class="tabletext" style="font-size: xx-small;">
                <b><i>${uiLabelMap.OrderAdjustment}</i>:</b>
                <b>${orderItemAdjustment.type.get("description",locale)}</b>:
                ${orderItemAdjustment.description?if_exists}
                <#if orderItemAdjustment.comments?has_content>(${orderItemAdjustment.comments})</#if>
                <#if orderItemAdjustment.productPromoId?has_content><a href="/catalog/control/EditProductPromo?productPromoId=${orderItemAdjustment.productPromoId}&amp;externalLoginKey=${externalLoginKey}">${orderItemAdjustment.productPromo.promoName}</a></#if>
                <#if orderItemAdjustment.isSalesTax()>
                  <#if orderItemAdjustment.primaryGeoId?has_content>
                    <#if orderItemAdjustment.primaryGeo.geoName?has_content>
                      <b>${uiLabelMap.OrderJurisdiction}:</b> ${orderItemAdjustment.primaryGeo.geoName} [${orderItemAdjustment.primaryGeo.abbreviation?if_exists}]
                    </#if>
                    <#if orderItemAdjustment.secondaryGeoId?has_content>
                      (<b>${uiLabelMap.CommonIn}:</b> ${orderItemAdjustment.secondaryGeo.geoName} [${orderItemAdjustment.secondaryGeo.abbreviation?if_exists}])
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
                <@ofbizCurrency amount=orderItemAdjustment.calculateAdjustment(item) isoCode=order.currencyUom/>
              </div>
            </td>
            <td>&nbsp;</td>
          </tr>
        </#list>

        <#-- now show price info per line item -->
        <#if item.priceInfos?has_content>
          <tr><td>&nbsp;</td></tr>
          <#list item.priceInfos as orderItemPriceInfo>
            <tr>
              <td align="right" colspan="2">
                <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.ProductPriceRuleNameId}</i>:</b> [${orderItemPriceInfo.productPriceRuleId?if_exists}:${orderItemPriceInfo.productPriceActionSeqId?if_exists}] ${orderItemPriceInfo.description?if_exists}</div>
              </td>
              <td>&nbsp;</td>
              <td align="right">
                <div class="tabletext" style="font-size: xx-small;">
                  <@ofbizCurrency amount=orderItemPriceInfo.modifyAmount isoCode=order.currencyUom/>
                </div>
              </td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
            </tr>
          </#list>
        </#if>

        <!-- display the ship before/after dates -->
        <#if item.isPhysical() && item.shipAfterDate?exists>
          <tr>
            <td align="right" colspan="2">
              <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipAfterDate}</i>:</b> ${getLocalizedDate(item.shipAfterDate, "DATE")}</div>
            </td>
          </tr>
        </#if>
        <#if item.shipBeforeDate?exists>
          <tr>
            <td align="right" colspan="2">
              <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipBeforeDate}</i>:</b> ${getLocalizedDate(item.shipBeforeDate, "DATE")}</div>
            </td>
          </tr>
        </#if>

        <#-- now show ship group info per line item -->
        <#if item.isPhysical()>
          <#assign orderItemShipGroupAssocs = orderItemGV.getRelated("OrderItemShipGroupAssoc")?if_exists/>
          <#if orderItemShipGroupAssocs?has_content>
            <#list orderItemShipGroupAssocs as shipGroupAssoc>
              <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")/>
              <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists/>
              <#assign shipGroupQty = shipGroupAssoc.quantity - shipGroupAssoc.cancelQuantity?default(0) />
              <#if shipGroupQty gt 0>
                <tr>
                  <td align="right" colspan="2">
                    <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                  </td>
                  <td align="center">
                    <div class="tabletext" style="font-size: xx-small;">${shipGroupQty?string.number}&nbsp;</div>
                  </td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td align="right" valign="top">
                    &nbsp;
                  </td>
                </tr>
              </#if>
            </#list>
          </#if>
        </#if>

        <#-- now show inventory reservation info per line item -->
        <#if item.isPhysical() && item.shipGroupInventoryReservations?has_content>
          <#list item.shipGroupInventoryReservations as orderItemShipGrpInvRes>
            <#assign itemIndex = item_index />
            <#assign reservIndex = orderItemShipGrpInvRes_index />

            <script type="text/javascript">
              reReservationWidgetParameters["inventoryItemId_${item_index}_${orderItemShipGrpInvRes_index}"] = "${orderItemShipGrpInvRes.getInventoryItemId()}";
              reReservationWidgetParameters["shipGroupSeqId_${item_index}_${orderItemShipGrpInvRes_index}"] = "${orderItemShipGrpInvRes.getShipGroupSeqId()}";
              reReservationWidgetParameters["quantity_${item_index}_${orderItemShipGrpInvRes_index}"] = "${orderItemShipGrpInvRes.getQuantity().toString()}";
            </script>

            <tr>
              <td align="right" colspan="2">
                <div class="tabletext" style="font-size: xx-small;">
                  <b><i>${uiLabelMap.ProductInventoryItem} </i></b>
                  <a href="/warehouse/control/EditInventoryItem?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="linktext" style="font-size: xx-small;">${orderItemShipGrpInvRes.inventoryItemId}</a>
                  <i> ${uiLabelMap.CrmReservedTo} ${orderItemShipGrpInvRes?if_exists.getInventoryItem()?if_exists.getFacility()?if_exists.getFacilityName()?if_exists}</i><b>
                  <i><@gwtWidget id="${makeName('reReserveItemDialog', itemIndex, reservIndex)}" class="inlinebuttontext"/></i></b>
                  <b><i>${uiLabelMap.OrderShipGroup}</i>:</b> ${orderItemShipGrpInvRes.shipGroupSeqId}
                  <#assign inventoryItem = orderItemShipGrpInvRes.inventoryItem?if_exists />
                  <#assign lotId = (inventoryItem.lotId)?if_exists />
                  <#if lotId?has_content>
                    <b><i>${uiLabelMap.ProductLotId}</i>:</b>
                    <a href="/warehouse/control/lotDetails?lotId=${lotId}&amp;externalLoginKey=${externalLoginKey}" class="linktext" style="font-size: xx-small;">${lotId}</a>
                  </#if>
                </div>
              </td>
              <td align="center">
                <div class="tabletext" style="font-size: xx-small;">${orderItemShipGrpInvRes.quantity?string.number}&nbsp;</div>
              </td>
              <td class="tabletext">
                <#if orderItemShipGrpInvRes.quantityNotAvailable gt 0>
                  <span style="color: red;">[${orderItemShipGrpInvRes.quantityNotAvailable?string.number}&nbsp;${uiLabelMap.OrderBackOrdered}]</span>
                </#if>
                &nbsp;
              </td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
            </tr>
          </#list>
        </#if>

        <#-- now show planned shipment info per line item -->
        <#if item.isPhysical()>
          <#if item.orderShipments?has_content>
            <#list item.orderShipments as orderShipment>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderPlannedInShipment}</i>: </b><a target="facility" href="/warehouse/control/ViewShipment?shipmentId=${orderShipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="linktext" style="font-size: xx-small;">${orderShipment.shipmentId}</a>: ${orderShipment.shipmentItemSeqId}</div>
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

        <#-- now show item issuances per line item -->
        <#if item.isPhysical()>
          <#if item.itemIssuances?has_content>
            <#list item.itemIssuances as itemIssuance>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;">
                    <#if itemIssuance.shipmentId?has_content>
                      <b><i>${uiLabelMap.OrderIssuedToShipmentItem}</i>:</b>
                      <a target="facility" href="/warehouse/control/ViewShipment?shipmentId=${itemIssuance.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="linktext" style="font-size: xx-small;">${itemIssuance.shipmentId}</a>:${itemIssuance.shipmentItemSeqId?if_exists}
                    <#else>
                      <b><i>${uiLabelMap.OrderIssuedWithoutShipment}</i></b>
                    </#if>
                  </div>
                </td>
                <td align="center">
                  <div class="tabletext" style="font-size: xx-small;">${itemIssuance.quantity?string.number}&nbsp;</div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
              </tr>
            </#list>
          </#if>
        </#if>

      </#if>
      </#list>
    </#if>

    <#-- subtotal -->
    <@sectionSepBar />
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderItemsSubTotal}</b></div></td>
      <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=order.itemsSubTotal isoCode=order.currencyUom/></div></td>
    </tr>
    <#-- tax adjustments -->
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalSalesTax}</b></div></td>
      <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=order.taxAmount isoCode=order.currencyUom/></div></td>
    </tr>


    <#macro displayAdjustment adjustment>
      <#assign adjustmentAmount = adjustment.calculateAdjustment(order)/>
      <#if adjustmentAmount != 0>
        <tr>
          <td align="right" colspan="4">
            <div class="tabletext"><b>${adjustment.type.get("description",locale)}</b> ${adjustment.comments?if_exists} ${adjustment.description?if_exists} : </div>
          </td>
          <td align="right" nowrap="nowrap">
            <div class="tabletext"><@ofbizCurrency amount=adjustmentAmount isoCode=order.currencyUom/></div>
          </td>
          <td>&nbsp;</td>
        </tr>
      </#if>
    </#macro>

    <#-- shipping adjustments -->
    <tr><td colspan="8"><hr class="sepbar"/></td></tr>
    <#list order.shippingAdjustments as shippingAdjustment>
      <@displayAdjustment adjustment=shippingAdjustment/>
    </#list>
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalShippingAndHandling}</b></div></td>
      <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=order.shippingAmount isoCode=order.currencyUom/></div></td>
    </tr>

    <#-- other adjustments -->
    <tr><td colspan="8"><hr class="sepbar"/></td></tr>
    <#list order.nonShippingAdjustments as otherAdjustment>
      <@displayAdjustment adjustment=otherAdjustment/>
    </#list>
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalOtherOrderAdjustments}</b></div></td>
      <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=order.otherAdjustmentsAmount isoCode=order.currencyUom/></div></td>
    </tr>
    <@sectionSepBar/>

    <#-- grand total -->
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderOrderTotal}</b></div></td>
      <td align="right" nowrap="nowrap">
        <div class="tabletext"><@ofbizCurrency amount=order.total isoCode=order.currencyUom/></div>
      </td>
    </tr>
    <tr>
      <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalDue}</b></div></td>
      <td align="right" nowrap="nowrap">
        <div class="tabletext"><@ofbizCurrency amount=order.openAmount isoCode=order.currencyUom/></div>
      </td>
    </tr>

  </table>
</@flexAreaClassic>

</#if> <#-- end of if order?exists -->
