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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>
<#include "PicklistReportMacros.fo.ftl">

<!-- It is important to set defaults for facilityLocation and facilityLocationInfo in case the picklisted item has no location defined in facility.
  Because these defaults are scalars, we must then use the ?is_hash check directive as well before trying to access them -->
<!--
     - picklist
     - facility
     - statusItem
     - statusValidChangeToDetailList
     - picklistRoleInfoList (picklistRole, partyNameView, roleType)
     - picklistStatusHistoryInfoList (picklistStatusHistory, statusItem, statusItemTo)
     - picklistBinInfoList
       - picklistBin
       - primaryOrderHeader
       - primaryOrderItemShipGroup
       - picklistItemInfoList (picklistItem, picklistBin, orderItem, product, inventoryItemAndLocation, orderItemShipGrpInvRes, itemIssuanceList)
-->

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>

    <#-- master layout specifies the overall layout of the pages and its different sections. -->
    <fo:layout-master-set>
        <fo:simple-page-master master-name="my-page"
            margin-top="1in" margin-bottom="0in"
            margin-left="20mm" margin-right="20mm">
            <fo:region-body margin-top="1.5in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-before extent="1.5in"/>  <#-- a header -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

    <fo:page-sequence master-reference="my-page">

        <fo:static-content flow-name="xsl-region-before">
            <@opentapsHeaderFO>
            <fo:block><fo:leader/></fo:block>
            </@opentapsHeaderFO>
        </fo:static-content>

        <@opentapsFooterFO />

        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">

            <#if picklistInfo?has_content>
                <fo:block font-size="12pt">${uiLabelMap.ProductPickList} ${picklistInfo.picklist.picklistId} ${uiLabelMap.CommonIn} ${uiLabelMap.ProductFacility} ${picklistInfo.facility.facilityName} <fo:inline font-size="8pt">[${picklistInfo.facility.facilityId}]</fo:inline></fo:block>
                <#if picklistInfo.shipmentMethodType?has_content>
                    <fo:block font-size="10pt">${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentMethodType} ${picklistInfo.shipmentMethodType.description?default(picklistInfo.shipmentMethodType.shipmentMethodTypeId)}</fo:block>
                </#if>
                <fo:block><fo:leader/></fo:block>
            </#if>

            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="90pt"/>
                <fo:table-column column-width="220pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-column column-width="80pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductLocation}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductToPick}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductQuantityToBin}#</fo:block></fo:table-cell>

                      <#-- Not display details here, just the summary info for the bins
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderOrderItems}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductInventoryItems}</fo:block></fo:table-cell>
                      -->
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#--
                      2. facilityLocationInfoList (facilityLocation, product, pickQuantity, picklistBinInfoList (picklistBin, quantity), picklistItemInfoList (picklistItem, picklistBin, orderItem, product, inventoryItemAndLocation, orderItemShipGrpInvRes, itemIssuanceList))
                      3. noLocationProductInfoList (product, pickQuantity, picklistBinInfoList (picklistBin, quantity), picklistItemInfoList (picklistItem, picklistBin, orderItem, product, inventoryItemAndLocation, orderItemShipGrpInvRes, itemIssuanceList))
                    -->
                    <#if facilityLocationInfoList?has_content || noLocationProductInfoList?has_content>
                        <#assign rowColor = "white">
                        <#if facilityLocationInfoList?has_content>
                        <#list facilityLocationInfoList as facilityLocationInfo>
                            <@pickInfoDetail pickQuantity=facilityLocationInfo.pickQuantity picklistBinInfoList=facilityLocationInfo.picklistBinInfoList product=facilityLocationInfo.product facilityLocation=facilityLocationInfo.facilityLocation facilityLocationInfo=facilityLocationInfo/>
                          <#-- Not display details here, just the summary info for the bins
                            <#list facilityLocationInfo.picklistItemInfoList as picklistItemInfo>
                                <@picklistItemInfoDetail picklistItemInfo=picklistItemInfo product=picklistItemInfo.product facilityLocation=facilityLocationInfo.facilityLocation/>
                            </#list>
                          -->
                        </#list>
                        </#if>

                        <#if noLocationProductInfoList?has_content>
                        <#list noLocationProductInfoList as noLocationProductInfo>
                            <@pickInfoDetail pickQuantity=noLocationProductInfo.pickQuantity picklistBinInfoList=noLocationProductInfo.picklistBinInfoList product=noLocationProductInfo.product facilityLocation=null facilityLocationInfo=null/>
                          <#-- Not display details here, just the summary info for the bins
                            <#list noLocationProductInfo.picklistItemInfoList as picklistItemInfo>
                                <@picklistItemInfoDetail picklistItemInfo=picklistItemInfo product=noLocationProductInfo.product facilityLocation=null/>
                            </#list>
                          -->
                        </#list>
                        </#if>
                    <#else>
                        <fo:table-row font-weight="bold">
                            <fo:table-cell><fo:block>${uiLabelMap.ProductNoInventoryFoundToPick}.</fo:block></fo:table-cell>
                        </fo:table-row>
                    </#if>
                </fo:table-body>
            </fo:table>
            </fo:block>
        </fo:flow>
    </fo:page-sequence>

<#if picklistInfo?has_content>
    <#list picklistInfo.picklistBinInfoList as picklistBinInfo>
        <#assign rowColor = "white">
        <#assign picklistBin = picklistBinInfo.picklistBin>
        <#assign picklistItemInfoList = picklistBinInfo.picklistItemInfoList?if_exists>

        <fo:page-sequence master-reference="my-page">

            <fo:static-content flow-name="xsl-region-before">
                <@opentapsHeaderFO>
                <fo:block><fo:leader/></fo:block>
                </@opentapsHeaderFO>
            </fo:static-content>

            <@opentapsFooterFO />

            <fo:flow flow-name="xsl-region-body" font-family="Helvetica">

                <fo:table table-layout="fixed" font-size="10pt">
                    <fo:table-column column-width="proportional-column-width(2)"/>
                    <fo:table-column column-width="proportional-column-width(2)"/>
                    <fo:table-column column-width="proportional-column-width(1)"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:table table-layout="fixed">
                                    <fo:table-column column-width="proportional-column-width(2)"/>
                                    <fo:table-column column-width="proportional-column-width(3)"/>
                                    <fo:table-body>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.ProductBinNumber}</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block>${picklistBin.binLocationNumber}</fo:block></fo:table-cell>
                                        </fo:table-row>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonFor}:</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block>${picklistBinInfo.customerName?if_exists}</fo:block></fo:table-cell>
                                        </fo:table-row>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OpentapsPONumber}</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block>${picklistBinInfo.customerPoNumber?default("N/A")}</fo:block></fo:table-cell>
                                        </fo:table-row>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderOrder}:</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block>${picklistBinInfo.primaryOrderHeader.orderId} - ${picklistBinInfo.primaryOrderItemShipGroup.shipGroupSeqId}</fo:block></fo:table-cell>
                                        </fo:table-row>
                                        <fo:table-row>
                                            <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderShipVia}:</fo:block></fo:table-cell>
                                            <fo:table-cell><fo:block>${picklistBinInfo.shipVia?if_exists}</fo:block></fo:table-cell>
                                        </fo:table-row>
                                    </fo:table-body>
                                </fo:table>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block font-weight="bold">Ship To:</fo:block>
                                <@displayAddress postalAddress=picklistBinInfo.shippingAddress />
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block text-align="right">
                                    <fo:instream-foreign-object>
                                        <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                            message="${picklistBinInfo.primaryOrderHeader.orderId}/${picklistBinInfo.primaryOrderItemShipGroup.shipGroupSeqId}">
                                            <barcode:code39>
                                                <barcode:height>8mm</barcode:height>
                                            </barcode:code39>
                                        </barcode:barcode>
                                    </fo:instream-foreign-object>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>
                <fo:block><fo:leader/></fo:block>

                <fo:block space-after.optimum="10pt" font-size="10pt">
                <fo:table>
                    <fo:table-column column-width="60pt"/>
                    <fo:table-column column-width="180pt"/>
                    <fo:table-column column-width="50pt"/>
                    <fo:table-column column-width="70pt"/>
                    <fo:table-column column-width="40pt"/>
                    <fo:table-column column-width="40pt"/>
                    <fo:table-header>
                        <fo:table-row font-weight="bold">
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderOrderItem}</fo:block></fo:table-cell>
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductToPack}</fo:block></fo:table-cell>
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductInventoryItem}</fo:block></fo:table-cell>
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderOrdered}</fo:block></fo:table-cell>
                            <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderBackOrdered}</fo:block></fo:table-cell>
                        </fo:table-row>
                    </fo:table-header>
                    <fo:table-body>
                        <#list picklistItemInfoList as picklistItemInfo>
                            <#assign picklistItem = picklistItemInfo.picklistItem>
                            <#assign orderItem = picklistItemInfo.orderItem>
                            <#assign product = picklistItemInfo.product>
                            <#assign orderItemShipGrpInvRes = picklistItemInfo.orderItemShipGrpInvRes?if_exists>
                            <fo:table-row>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block><#--${picklistItem.orderId}:${picklistItem.shipGroupSeqId}:-->${picklistItem.orderItemSeqId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <#if product?has_content>
                                        <fo:block>${product.internalName?default("Internal Name Not Set!")} [${product.productId}]</fo:block>
                                    <#else/>
                                        <fo:block>&nbsp;</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${picklistItem.quantity}</fo:block>
                                </fo:table-cell>
                                <#if orderItemShipGrpInvRes?has_content>
                                    <#assign quantityAvailable = orderItemShipGrpInvRes.quantity?default(0) - orderItemShipGrpInvRes.quantityNotAvailable?default(0)>
                                    <fo:table-cell padding="2pt" background-color="${rowColor}">
                                        <fo:block>${orderItemShipGrpInvRes.inventoryItemId}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt" background-color="${rowColor}">
                                        <fo:block>${quantityAvailable}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt" background-color="${rowColor}">
                                        <fo:block>${orderItemShipGrpInvRes.quantityNotAvailable?default(0)}</fo:block>
                                    </fo:table-cell>
                                <#else>
                                    <fo:table-cell padding="2pt" background-color="${rowColor}" number-columns-spanned="3">
                                        <fo:block>${uiLabelMap.ProductNoInventoryReservation}</fo:block>
                                    </fo:table-cell>
                                </#if>
                            </fo:table-row>
                            <#-- toggle the row color -->
                            <#if rowColor == "white">
                                <#assign rowColor = "#D4D0C8">
                            <#else>
                                <#assign rowColor = "white">
                            </#if>
                        </#list>
                    </fo:table-body>
                </fo:table>
                </fo:block>
                <#if picklistBinInfo.primaryOrderItemShipGroup.giftMessage?has_content>
                    <fo:block space-after.optimum="10pt" font-size="10pt">
                    <fo:table>
                        <fo:table-column column-width="450pt"/>
                        <fo:table-body>
                            <fo:table-row font-weight="bold">
                                <fo:table-cell>
                                    <fo:block>${uiLabelMap.OrderGiftMessage}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <fo:table-row >
                                <fo:table-cell>
                                    <fo:block>${picklistBinInfo.primaryOrderItemShipGroup.giftMessage}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-body>
                    </fo:table>
                    </fo:block>
                </#if>

                <#if ! picklistBinInfo_has_next>
                  <fo:block id="theEnd"/>  <#-- Put this at end of flow so that footer knows how many pages there are. -->
                </#if>
           </fo:flow>
        </fo:page-sequence>
    </#list>
</#if>

</fo:root>
</#escape>
