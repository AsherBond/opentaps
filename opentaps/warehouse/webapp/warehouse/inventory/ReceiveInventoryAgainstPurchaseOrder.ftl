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

<#-- This file has been modified from the version included with the Apache-licensed OFBiz product application -->
<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script type="text/javascript">
    function rowClassChange(/*Element*/ input, /*Number*/ rowIndex) {
        // trim the input and replace empty by "0"
        if (input.value != null && input.value != "0") {
            input.value = input.value.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
            input.value = input.value.replace(/^00*/, '');
            if (input.value == "") input.value = "0";
        }
        // color the row according to the value: 0=>normal positive_integer=>green invalid=>red
        if (input.value != null && input.value != "0") {            
            if(parseInt(input.value) != input.value - 0 || parseInt(input.value) < 0) {
              input.parentNode.parentNode.className = 'rowLightRed';
            } else {
              input.parentNode.parentNode.className = 'rowLightGreen';
            }
        } else {
            input.parentNode.parentNode.className = rowIndex % 2 == 0 ? 'rowWhite' : 'rowLightGray';
        }
    }
</script>
<#assign productId = parameters.productId?if_exists/>

<form name="ReceiveInventoryAgainstPurchaseOrder" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
    <input type="hidden" name="clearAll" value="Y"/>
    <div class="tabletext" style="float: left;">
        ${uiLabelMap.ProductOrderId} : <@inputLookup name="purchaseOrderId" lookup="LookupPurchaseOrder" form="ReceiveInventoryAgainstPurchaseOrder" default=orderId?if_exists/>
        ${uiLabelMap.ProductOrderShipGroupId} : <@inputText name="shipGroupSeqId" size="20" default=shipGroupSeqId?default("00001")/>
        <input type="submit" value="${uiLabelMap.CommonReceive}" class="smallSubmit"/>
    </div>
    <#if orderId?has_content><div class="subMenuBar"><a class="subMenuButton" href="<@ofbizUrl>fulfilledBackOrders.pdf?orderId=${orderId}</@ofbizUrl>">PDF</a></div></#if>
    <div class="spacer"></div>
</form>
    
<div class="errorMessage" style="margin:5px 0px 5px 5px">
    <#if errorMessage?has_content>
        ${errorMessage}
    <#elseif orderId?has_content && shipGroupSeqId?has_content && !orderHeader?exists>
        <@expandLabel label="WarehouseErrorOrderIdAndShipGroupSeqIdNotFound" params={"orderId", orderId, "shipGroupSeqId", shipGroupSeqId} />
        <#assign totalAvailableToReceive = 0/>
    <#elseif orderId?has_content && !orderHeader?exists>
        <@expandLabel label="ProductErrorOrderIdNotFound" params={"orderId", orderId} />
        <#assign totalAvailableToReceive = 0/>
    <#elseif orderHeader?exists && orderHeader.orderTypeId != "PURCHASE_ORDER">
        <@expandlabel label="ProductErrorOrderNotPurchaseOrder" params={"orderId", orderId} />
        <#assign totalAvailableToReceive = 0/>
    <#elseif orderHeader?exists && orderHeader.statusId != "ORDER_APPROVED" && orderHeader.statusId != "ORDER_COMPLETED">
        <@expandLabel label="WarehouseErrorOrderNotApproved" params={"orderId", orderId} />
        <#assign totalAvailableToReceive = 0/>
    <#elseif ProductReceiveInventoryAgainstPurchaseOrderProductNotFound?exists>
        <@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderProductNotFound" params={"productId", productId, "orderId", orderId} />
        <script type="text/javascript">window.onload=function(){alert('<@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderProductNotFound" params={"productId", productId?default(""), "orderId", orderId} />')};</script>
    <#elseif ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive?exists>
        <@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive" params={"newQuantity", newQuantity?if_exists, "productId", productId?default("")} />
        <script type="text/javascript">window.onload=function(){alert('<@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive" params={"newQuantity", newQuantity?if_exists, "productId", productId?default("")} />')};</script>
    </#if>
</div>

<#if ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder?exists && ! ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive?exists && productId?exists>
    <div class="errorMessage" style="color:green;margin:5px 0px 5px 5px">
        <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder?interpret><@uiLabelWithVar/>
        <@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder" params={"quantityToBackOrder", quantityToBackOrder?if_exists, "quantityToReceive", quantityToReceive?if_exists, "productId", productId?default("")}/>
        <script type="text/javascript">window.onload=function(){alert('<@expandLabel label="ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder" params={"quantityToBackOrder", quantityToBackOrder?if_exists, "quantityToReceive", quantityToReceive?if_exists, "productId", productId?default("")}/>')};</script>
    </div>
</#if>

<#if ! error?exists>

    <#assign itemsAvailableToReceive = itemsAvailableToReceive?default(totalAvailableToReceive?default(0) &gt; 0)/>
    <#if orderItemDatas?exists>
        <#assign totalReadyToReceive = 0/>
        <form action="<@ofbizUrl>issueOrderItemToShipmentAndReceiveAgainstPO?clearAll=Y</@ofbizUrl>" method="post" name="selectAllForm">
            <input type="hidden" name="facilityId" value="${facilityId}"/>
            <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
            <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId}"/>
            <input type="hidden" name="currencyUomId" value="${orderHeader.currencyUomId?default("")}"/>
            <input type="hidden" name="ownerPartyId" value="${(facility.ownerPartyId)?if_exists}"/>
            <@inputHidden name="completePurchaseOrder" value="N"/>
            <table class="listTable">
                <tr class="listTableHeader">
                    <td><div class="tableheadtext">${uiLabelMap.WarehouseSupplierProduct}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
                    <td class="fieldWidth50"><div class="tableheadtext">${uiLabelMap.WarehouseNetOrdered}</div></td>
                    <td class="fieldWidth50"><div class="tableheadtext">${uiLabelMap.WarehouseCurrentBackOrderedQty}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.CommonReceived}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.ProductOpenQuantity}</div></td>
                    <td style="width:125px"><div class="tableheadtext">${uiLabelMap.WarehouseFulfilledBackOrders}</div></td>
                    <#if itemsAvailableToReceive>
                        <td><div class="tableheadtext">${uiLabelMap.CommonReceive}</div></td>
                        <td><div class="tableheadtext">${uiLabelMap.ProductInventoryItemType}</div></td>
                    </#if>
                    <td><div class="tableheadtext">${uiLabelMap.WarehouseLot}</div></td>
                    <#if itemsAvailableToReceive>
                        <td align="right">
                            <div class="tableheadtext">${uiLabelMap.CommonReceive}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');"></div>
                        </td>
                    </#if>
                </tr>
                <#list orderItemDatas.values()?default({}) as orderItemData>
                    <#assign orderItem = orderItemData.orderItem>
                    <#assign product = orderItemData.product?if_exists>
                    <#assign itemShipGroupSeqId = orderItemData.shipGroupSeqId?if_exists>
                    <#assign totalQuantityReceived = orderItemData.totalQuantityReceived?default(0)>
                    <#assign availableToReceive = orderItemData.availableToReceive?default(0)>
                    <#assign backOrderedQuantity = orderItemData.backOrderedQuantity?default(0)>
                    <#assign fulfilledOrderIds = orderItemData.fulfilledOrderIds>
                    <#assign lotIds = orderItemData.lotIds>
                    <#assign quantityToReceive = 0>
                    <#if itemsAvailableToReceive && availableToReceive &gt; 0 >
                        <#if itemQuantitiesToReceive?exists && itemQuantitiesToReceive.get(orderItem.orderItemSeqId)?exists>
                            <#assign quantityToReceive = itemQuantitiesToReceive.get(orderItem.orderItemSeqId)>
                        </#if>
                        <#assign totalReadyToReceive = totalReadyToReceive + quantityToReceive/>
                    </#if>
                    <#if quantityToReceive &gt; 0>
                        <#assign rowClass = "rowLightGreen"/>
                    <#else>
                        <#assign rowClass = tableRowClass(orderItemData_index)/>
                    </#if>
                    <tr class="${rowClass}">
                        <td>
                            <div class="tabletext">
                                ${orderItemData.supplierProductName?if_exists}<br/>
                                [${orderItemData.supplierProductId?default("N/A")}]
                            </div>
                        </td>
                        <td>
                            <div class="tabletext">
                                ${(product.internalName)?if_exists}<br/>
                                [${orderItem.productId?default("N/A")}]
                                <#assign upcaLookup = Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", product.productId, "goodIdentificationTypeId", "UPCA")/>
                                <#assign upca = delegator.findByPrimaryKeyCache("GoodIdentification", upcaLookup)?if_exists/>
                                <#if upca?has_content>
                                    <br/>
                                    ${upca.idValue?if_exists}
                                </#if>
                            </div>
                        </td>
                        <td>
                            <div class="tabletext">
                                ${orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)}
                            </div>
                        </td>
                        <td>
                            <div class="tabletext ${(backOrderedQuantity &gt; 0)?string(" errorMessage","")}">
                                ${backOrderedQuantity}
                            </div>
                        </td>
                        <td>
                            <div class="tabletext">${totalQuantityReceived}</div>
                        </td>
                        <td>
                            <div class="tabletext">
                                ${orderItem.quantity - orderItem.cancelQuantity?default(0) - totalQuantityReceived}
                            </div>
                        </td>
                        <td>
                            <div class="tabletext">
                                <#if fulfilledOrderIds?has_content>
                                    <#list fulfilledOrderIds?sort as fulfilledOrderId>
                                        ${fulfilledOrderId}&nbsp;[<a href="<@ofbizUrl>PackOrder?orderId=${fulfilledOrderId}</@ofbizUrl>">${uiLabelMap.WarehousePackOrder}</a>]<br/>
                                    </#list>
                                </#if>
                                &nbsp;
                            </div>
                        </td>
                        <#if itemsAvailableToReceive>
                          <#if availableToReceive gt 0>
                            <td>
                                <input type="hidden" name="orderItemSeqId_o_${orderItemData_index}" value="${orderItem.orderItemSeqId}"/>
                                <input type="hidden" name="productId_o_${orderItemData_index}" value="${(product.productId)?if_exists}"/>
                                <input type="hidden" name="unitCost_o_${orderItemData_index}" value="${orderItem.unitPrice?default(0)}"/>
                                <input type="hidden" name="quantityRejected_o_${orderItemData_index}" value="0"/>
                                <input type="text" class='inputBox' size="5" name="quantityAccepted_o_${orderItemData_index}" id="quantityAccepted_o_${orderItemData_index}" value="${orderItemData.receiveDefQty?default(0)}" onchange="rowClassChange(this, ${orderItemData_index})"/>
                            </td>
                            <td>              
                                <select name="inventoryItemTypeId_o_${orderItemData_index}" class="selectBox">
                                  <#list inventoryItemTypes as inventoryItemType>
                                  <option value="${inventoryItemType.inventoryItemTypeId}"
                                      <#if facility?exists && (facility.defaultInventoryItemTypeId?has_content) && (inventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                                          selected="selected"
                                      </#if>    
                                  >${inventoryItemType.get("description",locale)?default(inventoryItemType.inventoryItemTypeId)}</option>
                                  </#list>
                                </select>
                            </td>
                          <#else>
                            <td colspan="2">&nbsp;</td>
                          </#if>
                        </#if>
                        <td>
                            <div class="tabletext">
                                <#if lotIds?has_content>
                                    <#list lotIds?sort as lotId>
                                        <a href="<@ofbizUrl>lotDetails?lotId=${lotId}</@ofbizUrl>">${lotId}</a><br/>
                                    </#list>
                                </#if>
                                <#if itemsAvailableToReceive && availableToReceive &gt; 0 >
                                    <input type="text" class='inputBox' size="10" name="lotId_o_${orderItemData_index}" id="lotId_o_${orderItemData_index}" value=""/>
                                    <a href="javascript:call_fieldlookup2(document.selectAllForm.lotId_o_${orderItemData_index},'createLotPopupForm');" class="buttontext">${uiLabelMap.CommonNew}</a>
                                </#if>
                            </div>
                        </td>
                        <#if itemsAvailableToReceive>
                            <td align="right">
                              <#if availableToReceive gt 0>
                                <input type="checkbox" name="_rowSubmit_o_${orderItemData_index}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                              </#if>
                            </td>
                        </#if>
                    </tr>
                </#list>
            </table>
            <#if itemsAvailableToReceive>
                <div style="width:100%; text-align:right; margin-top: 15px">
                    <@inputConfirm title=uiLabelMap.CommonClearAll href="ReceiveInventoryAgainstPurchaseOrder?purchaseOrderId=${orderId}&clearAll=Y"/>
                </div>
            </#if>
            <#if itemsAvailableToReceive>
                <div style="width:100%; text-align:right; margin-top: 10px">
                    <span class="tabletext">Receive PO items from Shipment: </span>
                    <select name="shipmentId" class="selectBox">
                        <option value="">${uiLabelMap.CommonNew}</option>
                        <#list shipments as shipment>
                          <option value="${shipment.shipmentId}" <#if primaryShipmentId?default("") == shipment.shipmentId>selected="selected"</#if>>${shipment.shipmentId}</option>
                        </#list>
                    </select>
                    <a class="smallSubmit" href="javascript:document.selectAllForm.completePurchaseOrder.value='N';document.selectAllForm.submit();">${uiLabelMap.WarehouseReceiveAndKeepOpen}</a>
                </div>
                <div style="width:100%; text-align:right; margin-top: 15px">
                    <@inputConfirm title=uiLabelMap.WarehouseReceiveAndClosePO form="selectAllForm" onClick="document.selectAllForm.completePurchaseOrder.value='Y'"/>
                </div>
            </#if>
        </form>
        <script language="JavaScript" type="text/javascript">selectAll('selectAllForm');</script>
    </#if>
    <#if itemsAvailableToReceive && totalReadyToReceive < totalAvailableToReceive>
        <div class="head3">${uiLabelMap.ProductReceiveInventoryAddProductToReceive}</div>
        <form name="addProductToReceive" method="post" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
            <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
            <div class="tabletext">
                <span class="tabletext">
                    ${uiLabelMap.ProductProductId}/${uiLabelMap.ProductGoodIdentification}/${uiLabelMap.FormFieldTitle_supplierProductId} <input type="text" class="inputBox" size="20" id="productId" name="productId" value=""/>
                    @
                    <input type="text" class="inputBox" name="quantity" size="6" maxlength="6" value="1" tabindex="0"/>
                    <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                </span>
            </div>
        </form>
        <script language="javascript">
            document.getElementById('productId').focus();
        </script>
    </#if>
</#if>
