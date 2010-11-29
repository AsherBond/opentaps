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

<#-- This file has been modified from the version included with the Apache licensed OFBiz product application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

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

<#if warehouseSecurity.hasFacilityPermission("WRHS_SHIP_PACK")>
    <#assign showInput = requestParameters.showInput?default("Y")>
    <#assign hideGrid = requestParameters.hideGrid?default("N")>    

    <#if (requestParameters.forceComplete?has_content && !shipmentId?has_content)>
        <#assign forceComplete = "true">
        <#assign showInput = "Y">
    </#if>

<div class="screenlet">
  <div class="screenlet-body">
    <div class="head2">${uiLabelMap.ProductPackOrder}&nbsp;${uiLabelMap.CommonIn}&nbsp;${facility.facilityName?if_exists} [${facilityId?if_exists}]</div>
    <#if shipmentId?has_content>
         <div class="tabletext">
           <p><b>${uiLabelMap.WarehouseDocumentsAvailableForShipment} ${shipmentId}</b>:</p> 
           <ul class="bulletList">
             <li><a href="<@ofbizUrl>/PackingSlip.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="linktext">${uiLabelMap.ProductPackingSlip}</a></li>
             <li><a href="<@ofbizUrl>/ShipmentBarCode.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="linktext">${uiLabelMap.WarehouseBarCode}</a></li>
             <#if hasInvoiceViewPermission>
              <#if invoiceIds?exists && invoiceIds?has_content>
              <#list invoiceIds as invoiceId>
               <li>
                 ${uiLabelMap.AccountingInvoice} #
                 <a href="<@ofbizUrl>invoice.pdf?invoiceId=${invoiceId}&amp;reportId=FININVOICE&amp;reportType=application/pdf</@ofbizUrl>" target="_blank" class="linktext">${invoiceId}</a> (PDF)</li>
               </li>
             </#list>
             </#if>
            </#if>
           </ul>
           <p><a href="<@ofbizUrl>QuickScheduleShipmentRouteSegment?shipmentId=${shipmentId}</@ofbizUrl>" class="linktext">${uiLabelMap.WarehouseScheduleThisShipment} ${shipmentId}</a></p>
         </div>
    </#if>
    <div>&nbsp;</div>

    <!-- select order form -->
    <form name="selectOrderForm" method="post" action="<@ofbizUrl>PackOrder</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
      <table border='0' cellpadding='2' cellspacing='0'>
        <tr>
          <td width="25%" align='right'><div class="tabletext">${uiLabelMap.ProductOrderId} #</div></td>
          <td width="1">&nbsp;</td>
          <td width="25%">
            <input type="text" class="inputBox" name="orderId" size="20" maxlength="20" value="${orderId?if_exists}"/>
            <span class="tabletext">/</span>
            <input type="text" class="inputBox" name="shipGroupSeqId" size="6" maxlength="6" value="${shipGroupSeqId?default("00001")}"/>
          </td>
          <td><div class="tabletext">${uiLabelMap.ProductHideGrid}:&nbsp;<input type="checkbox" name="hideGrid" value="Y" <#if (hideGrid == "Y")>checked=""</#if>></div></td>
          <td><div class='tabletext'>&nbsp;</div></td>
        </tr>
        <tr>
          <td colspan="2">&nbsp;</td>
          <td colspan="2">
            <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.selectOrderForm.submit();">
            <a href="javascript:document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
          </td>
        </tr>
      </table>
    </form>

    <form name="clearPackForm" method="post" action="<@ofbizUrl>ClearPackAll</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
    </form>
    <form name="incPkgSeq" method="post" action="<@ofbizUrl>SetNextPackageSeq</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
    </form>

    <#if showInput != "N" && orderHeader?exists && orderHeader?has_content>
      <hr class="sepbar"/>
      <div class='head2' style="float:left">${uiLabelMap.ProductOrderId} ${orderId?default("N/A")} / ${uiLabelMap.ProductOrderShipGroupId} ${shipGroupSeqId?default("N/A")}</div>
      <div class='head2' style="float:right; margin-right: 20px">${uiLabelMap.ProductCustomer}:&nbsp;${customerName?if_exists}
      <#if customerPoNumber?has_content><span style="margin-left: 20px">${uiLabelMap.OpentapsPONumber}:&nbsp;${customerPoNumber}</span></#if></div>
      <div class="spacer"></div>
      <#if orderItemShipGroup?has_content>
        <#assign postalAddress = orderItemShipGroup.getRelatedOne("PostalAddress")?if_exists>
        <#assign carrier = orderItemShipGroup.carrierPartyId?default("N/A")>
        <table border='0' cellpadding='4' cellspacing='4' width="100%">
          <tr>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.ProductShipToAddress}:</div>
              <div class="tabletext">
                <#if postalAddress?has_content>
                  <#if postalAddress.contactMechId == "_NA_">
                    ${uiLabelMap.WarehouseAddressUnknown}
                  <#else>
                    <b>${uiLabelMap.CommonTo}: </b>${postalAddress.toName?default("")}<br>
                    <#if postalAddress.attnName?has_content>
                      <b>${uiLabelMap.CommonAttn}: </b>${postalAddress.attnName}<br>
                    </#if>
                    ${postalAddress.address1}<br>
                    <#if postalAddress.address2?has_content>
                      ${postalAddress.address2}<br>
                    </#if>
                    ${postalAddress.city?if_exists}, ${postalAddress.stateProvinceGeoId?if_exists} ${postalAddress.postalCode?if_exists}<br>
                    ${postalAddress.countryGeoId}
                  </#if>
                <#else>
                  ${uiLabelMap.WarehouseNoPostalAddress}
                </#if>
              </div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.ProductCarrierShipmentMethod}:</div>
              <div class="tabletext">
                <#if carrier == "USPS">
                  <#assign color = "red">
                <#elseif carrier == "UPS">
                  <#assign color = "green">
                <#else>
                  <#assign color = "black">
                </#if>
                <#if carrier != "_NA_">
                  <font color="${color}">${carrier}</font>
                  &nbsp;
                </#if>
                ${orderItemShipGroup.shipmentMethodTypeId?default("??")}
              </div>
              <div>&nbsp;</div>
              <div class="tableheadtext">${uiLabelMap.ProductEstimatedShipCostForShipGroup}:</div>
              <#if orderShippingCharges?exists>
                  <div class="tabletext"><@ofbizCurrency amount=orderShippingCharges isoCode=orderReadHelper.getCurrency()?if_exists/></div>
              </#if>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.ProductShipping} ${uiLabelMap.ProductInstruction}:</div>
              <div class="tabletext">${orderItemShipGroup.shippingInstructions?default("(none)")}</div>
              <#if orderItemShipGroup.thirdPartyAccountNumber?has_content>
                <div>&nbsp;</div>
                <div class="tableheadtext">${uiLabelMap.WarehouseThirdPartyShippingAccount}:</div>
                <div class="tabletext">
                  ${orderItemShipGroup.thirdPartyAccountNumber?default("")}
                  (${orderItemShipGroup.thirdPartyPostalCode?default("")},&nbsp;${orderItemShipGroup.thirdPartyCountryGeoCode?default("")})
                </div>
              </#if>
            </td>
          </tr>
        </table>
        <div>&nbsp;</div>
      </#if>

      <div style="background-color: #EEE">

      <!-- auto grid form -->
      <#if showInput != "N" && hideGrid != "Y" && itemInfos?has_content && (totalAvailableToPack gt 0)>
        <hr class="sepbar"/> 
        <div class="head2" style="margin-bottom: 8px; padding-left: 8px;">${uiLabelMap.WarehouseItemsToBePacked}</div>
        <form name="multiPackForm" method="post" action="<@ofbizUrl>ProcessBulkPackOrder</@ofbizUrl>" style="margin: 0;">
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
          <input type="hidden" name="orderId" value="${orderId?if_exists}">
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}">
          <input type="hidden" name="originFacilityId" value="${facilityId?if_exists}">
          <input type="hidden" name="hideGrid" value="${hideGrid}"/>

          <table border='0' width="100%" cellpadding="2" cellspacing="0">
            <tr>
              <td>&nbsp;</td>
              <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</td>
              <td><div class="tableheadtext">${uiLabelMap.ProductDescription}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrdered}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.WarehouseQuantityQOH}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantityShipped}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.WarehousePacked}</td>
              <td>&nbsp;</td>
              <td align="center"><div class="tableheadtext">${uiLabelMap.ProductPackQty}</td>
              <#--td align="center"><div class="tableheadtext">${uiLabelMap.ProductPackedWeight}&nbsp;(${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval})</td-->
              <td align="center"><div class="tableheadtext">${uiLabelMap.ProductPackage}</td>
            </tr>
            <tr>
              <td colspan="11">
                <hr class="sepbar"/>
              </td>
            </tr>

            <#list itemInfos as orderItem>
              <#assign inputQty = Static["org.opentaps.common.order.UtilOrder"].getQuantityToPack(orderItem, shipGroupSeqId, facilityId, packingSession)/>
              <#if orderItem.cancelQuantity?exists>
                <#assign orderItemQuantity = orderItem.quantity - orderItem.cancelQuantity/>
              <#else>
                <#assign orderItemQuantity = orderItem.quantity/>
              </#if>
              <#if inputQty gt 0>
                  <tr>
                    <td><input type="checkbox" name="sel_${orderItem.orderItemSeqId}" value="Y" checked="checked"/></td>
                    <td><div class="tabletext">${orderItem.productId?default("N/A")}</td>
                    <td><div class="tabletext">${orderItem.itemDescription?if_exists}</td>
                    <td align="right"><div class="tabletext">${orderItemQuantity}</td>
                    <td align="right"><div class="tabletext">${qohByProductId?default({})[orderItem.productId]?default(0)}</td>
                    <td align="right"><div class="tabletext">${shippedQuantity?default(0)}</td>
                    <td align="right"><div class="tabletext">${packingSession.getPackedQuantity(orderId, orderItem.orderItemSeqId, shipGroupSeqId, orderItem.productId!)}</td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="center">
                      <input type="text" class="inputBox" size="7" name="qty_${orderItem.orderItemSeqId}" value="${inputQty}">
                    </td>
                    <td align="center">
                      <select name="pkg_${orderItem.orderItemSeqId}" class="selectBox">
                        <option value="1">${uiLabelMap.ProductPackage} 1</option>
                        <option value="2">${uiLabelMap.ProductPackage} 2</option>
                        <option value="3">${uiLabelMap.ProductPackage} 3</option>
                        <option value="4">${uiLabelMap.ProductPackage} 4</option>
                        <option value="5">${uiLabelMap.ProductPackage} 5</option>
                      </select>
                    </td>
                    <input type="hidden" name="prd_${orderItem.orderItemSeqId}" value="${orderItem.productId?if_exists}">
                    <input type="hidden" name="ite_${orderItem.orderItemSeqId}" value="${orderItem.orderItemSeqId}">
                  </tr>
              </#if>
            </#list>
            <tr><td colspan="11">&nbsp;</td></tr>
            <tr>
              <td colspan="11" align="right">
                <input type="submit" value="${uiLabelMap.WarehousePack}" class="smallSubmit">
              </td>
            </tr>
          </table>
        </form>
      </#if>
      
      <!-- manual per item form -->
      <#if showInput != "N" && (totalAvailableToPack gt 0)>
        <div>&nbsp;</div>
        <form name="singlePackForm" method="post" action="<@ofbizUrl>ProcessPackOrder</@ofbizUrl>" style='margin: 0;'>
          <input type="hidden" name="packageSeq" value="${packingSession.getCurrentPackageSeq()}"/>
          <input type="hidden" name="orderId" value="${orderId}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId}"/>
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
          <input type="hidden" name="hideGrid" value="${hideGrid}"/>
          <table border='0' cellpadding='2' cellspacing='0' width="100%">
            <tr>
              <td><div class="tabletext">${uiLabelMap.ProductProduct} #</div></td>
              <td width="1">&nbsp;</td>
              <td>
                <input type="text" class="inputBox" name="productId" size="20" maxlength="20" value=""/>
                <script type="text/javascript">document.singlePackForm.productId.focus()</script>
                <span class="tabletext">${uiLabelMap.CommonQty}</span>
                <input type="text" class="inputBox" name="quantity" size="6" maxlength="6" value="1"/>
                <input type="submit" class="smallSubmit" value="${uiLabelMap.ProductPackItem}">
              </td>
              <td><div class='tabletext'>&nbsp;</div></td>
              <td align="right">
                <div class="tabletext">
                  ${uiLabelMap.CommonCurrent} ${uiLabelMap.ProductPackage} ${uiLabelMap.CommonSequence}: <b>${packingSession.getCurrentPackageSeq()}</b>
                  <input type="button" class="smallSubmit" value="${uiLabelMap.CommonNext} ${uiLabelMap.ProductPackage}" onclick="javascript:document.incPkgSeq.submit();">
                </div>
              </td>
            </tr>
          </table>
        </form>
        <div>&nbsp;</div>
      </#if>

    </div>

      <!-- packed items display -->
      <#assign packedLines = packingSession.getLines()?if_exists>
      <#if packedLines?has_content>
        <@form name="clearPackLine" url="ClearPackLine" facilityId=facilityId! orderId="" orderItemSeqId="" shipGroupSeqId="" inventoryItemId="" packageSeqId="" productId="" />
        <hr class="sepbar"/>
        <div class="head2" style="margin-bottom: 8px; padding-left: 8px;">${uiLabelMap.WarehouseItemsAlreadyPacked}</div>
        <table border='0' width="100%" cellpadding='2' cellspacing='6'>
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.ProductPackage} #</td>
            <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</td>
            <td><div class="tableheadtext">${uiLabelMap.ProductDescription}</td>
            <td><div class="tableheadtext">${uiLabelMap.WarehouseLotOrSerial}</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.ProductPackedQty}</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.OpentapsGrossValue}</td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td colspan="8">
              <hr class="sepbar"/>
            </td>
          </tr>
          <#list packedLines as line>
            <#assign orderItem = orderReadHelper.getOrderItem(line.getOrderItemSeqId())?if_exists>
            <tr>
              <td><div class="tabletext">${line.getPackageSeq()}</td>
              <td><div class="tabletext">${line.getProductId()?default("N/A")}</td>
              <td><div class="tabletext">${(orderItem.itemDescription)?default("[N/A]")}</td>
              <#assign inventoryItem = inventoryItems.get(line)?if_exists/>
              <td>
                <div class="tabletext">
                  <#if inventoryItem?exists>
                  ${inventoryItem.lotId?if_exists} ${inventoryItem.serialNumber?if_exists}
                  </#if>
                </div>
              </td>
              <td align="right"><div class="tabletext">${line.getQuantity()}</td>
              <td align="right"><div class="tabletext"><@ofbizCurrency amount=line.getRawValue(delegator) isoCode=orderHeader.currencyUom /></td>
              <td align="right"><@submitFormLink form="clearPackLine" text=uiLabelMap.CommonClear orderId=line.getOrderId() orderItemSeqId=line.getOrderItemSeqId() shipGroupSeqId=line.getShipGroupSeqId() inventoryItemId=line.getInventoryItemId() packageSeqId=line.getPackageSeq() productId=line.getProductId() /></td>
            </tr>
          </#list>
          <#if packedLines.size() != 0>
          <tr><td colspan="8">&nbsp;</td></tr>
          <tr>
              <td colspan="8" align="right">
                  <input type="button" class="smallSubmit" value="${uiLabelMap.CommonClearAll}" onclick="javascript:document.clearPackForm.submit();"/>
              </td>
          </tr>
          </#if>
        </table>
      </#if>

      <!-- complete form -->
      <#if showInput != "N" && packedLines.size() != 0>
        <form name="completePackForm" method="post" action="<@ofbizUrl>CompletePack</@ofbizUrl>" style='margin: 0;'>
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
          <input type="hidden" name="forceComplete" value="${forceComplete?default('false')}"/>
          <input type="hidden" name="weightUomId" value="${defaultWeightUomId}"/>
          <input type="hidden" name="showInput" value="N"/>
          <hr class="sepbar">
          <div>&nbsp;</div>
          <table border='0' cellpadding='2' cellspacing='0' width="100%">
            <tr>
                <td valign="top">
                <#assign packageSeqIds = packingSession.getPackageSeqIds()/>
                <#if packageSeqIds?has_content>
                <table class="tabletext" width="90%">
                    <#if orderItemShipGroup?has_content>
                        <input type="hidden" name="shippingContactMechId" value="${orderItemShipGroup.contactMechId?if_exists}"/>
                        <input type="hidden" name="shipmentMethodTypeId" value="${orderItemShipGroup.shipmentMethodTypeId?if_exists}"/>
                        <input type="hidden" name="carrierPartyId" value="${orderItemShipGroup.carrierPartyId?if_exists}"/>
                        <input type="hidden" name="carrierRoleTypeId" value="${orderItemShipGroup.carrierRoleTypeId?if_exists}"/>
                        <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
                    </#if>
                    <tr>
                        <td class="tableheadtext">${uiLabelMap.ProductPackage} #</td>
                        <td class="tableheadtext">${uiLabelMap.OpentapsGrossValue}</td>
                        <td class="tableheadtext">${uiLabelMap.CommonWeight} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval})</td>
                        <td class="tableheadtext">${uiLabelMap.WarehousePackageBoxType}</td>
                        <td class="tableheadtext">${uiLabelMap.WarehousePackageTrackingCode}</td>
                    </tr>
                    <#list packageSeqIds as packageSeqId>
                    <tr>
                        <td>${packageSeqId}</td>
                        <td><@ofbizCurrency amount=packingSession.getRawPackageValue(delegator, packageSeqId) isoCode=orderHeader.currencyUom /></td>
                        <td><input type="text" class="inputBox" size="7" name="packageWeight_${packageSeqId}" value="${packingSession.getPackageWeight(packageSeqId?int)?if_exists}"></td>
                        <td>
                            <#assign boxTypeId = packingSession.getPackageBoxTypeOrDefaultId(packageSeqId?string)?default("")/>
                            <select name="packageBoxTypeId_${packageSeqId}" class="selectBox" style="width: 150px">
                                <option value="" ${("" == boxTypeId)?string("selected=\"selected\"","")}>&nbsp;</option>
                                <#list shipmentBoxTypes as shipmentBoxType>
                                <option value="${shipmentBoxType.shipmentBoxTypeId}" ${(shipmentBoxType.shipmentBoxTypeId == boxTypeId)?string("selected=\"selected\"","")}>${shipmentBoxType.description}</option>
                                </#list>
                            </select>
                        </td>
                        <td><input type="text" class="inputBox" size="15" name="packageTrackingCode_${packageSeqId}" value="${packingSession.getPackageTrackingCode(packageSeqId?string)?if_exists}"></td>
                    </tr>
                    </#list>
                </table>
                </#if>
              <td>
                <#if prorateShipping?default("") == "N">
                  <div class="tableheadtext">${uiLabelMap.ProductAdditionalShippingCharge}:</div>
                  <div>
                    <select name="additionalShippingChargeDescription" class="selectBox">
                      <#list [uiLabelMap.WarehouseShipping, uiLabelMap.WarehouseHandling, uiLabelMap.WarehouseCrating, uiLabelMap.WarehouseInsurance, uiLabelMap.WarehouseWireFee] as option>
                        <#assign selected = (option == parameters.additionalShippingChargeDescription?default(""))?string("selected=\"selected\"", "")/>
                        <option value="${option}" ${selected}>${option}</option>
                      </#list>
                    </select>
                  </div>
                  <div style="margin-bottom: 8px;">
                      <input type="text" class="inputBox" name="additionalShippingCharge" value="${packingSession.getAdditionalShippingCharge()?if_exists}" size="6"/>
                  <#if packageSeqIds?has_content>
                          <a href="javascript:document.completePackForm.action='<@ofbizUrl>calcPackSessionAdditionalShippingCharge</@ofbizUrl>';document.completePackForm.submit();" class="buttontext">${uiLabelMap.OpentapsEstimate}</a>
                  </#if>
                  <div>&nbsp;</div>
                </#if>
                <div class="tableheadtext">${uiLabelMap.ProductHandlingInstructions}:</div>
                <div>
                  <textarea name="handlingInstructions" class="inputBox" rows="2" cols="25">${packingSession.getHandlingInstructions()?if_exists}</textarea>
                </div>
              </td>
              <td align="right">
                <div>
                  <#assign buttonName = "${uiLabelMap.ProductComplete}">
                  <#if forceComplete?default("false") == "true">
                    <#assign buttonName = "${uiLabelMap.ProductCompleteForce}">
                  </#if>
                  <@inputConfirm title=buttonName form="completePackForm" />
                </div>
              </td>
            </tr>
          </table>
          <div>&nbsp;</div>
          <div class="tabletext"><div class="tooltip">${uiLabelMap.OpentapsGrossValueDisclaimer}</div></div>
        </form>
      </#if>

    </#if>


    <#if ! orderId?has_content>
      <script type="text/javascript">
        document.selectOrderForm.orderId.focus();
      </script>
    </#if>
  </div>
<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>
</div>
