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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>

    <#-- master layout specifies the overall layout of the pages and its different sections. -->
    <fo:layout-master-set>
        <fo:simple-page-master master-name="my-page"
            margin-top="1in" margin-bottom="0in"
            margin-left="20mm" margin-right="20mm">
            <fo:region-body margin-top="2in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-before extent="2in"/>  <#-- a header -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

    <#assign shipGroup = shipment.getRelatedOne("PrimaryOrderItemShipGroup")?if_exists>
    <#assign carrier = (shipGroup.carrierPartyId)?default("N/A")>

    <fo:page-sequence master-reference="my-page">

        <fo:static-content flow-name="xsl-region-before">
            <@opentapsHeaderFO>
                <fo:block><fo:leader/></fo:block>
                <fo:table table-layout="fixed">
                    <fo:table-column column-width="proportional-column-width(2)"/>
                    <fo:table-column column-width="proportional-column-width(3)"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="2">
                                <fo:block font-weight="bold" font-size="18pt">${uiLabelMap.WarehousePackingList}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${uiLabelMap.ProductShipmentId}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block text-align="right">${shipmentId}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${uiLabelMap.CommonOrder}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block text-align="right">${shipment.primaryOrderId?default("N/A")}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${uiLabelMap.WarehousePackages}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block text-align="right">${packages?size}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>${uiLabelMap.CommonDate}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block text-align="right"><@displayDateFO date=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="2">
                                <fo:block margin-top="10px" text-align="center">
                                    <fo:instream-foreign-object>
                                        <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                                message="${shipment.shipmentId}">
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
            </@opentapsHeaderFO>
        </fo:static-content>
        
        <@opentapsFooterFO />

        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
        <#if packages?has_content>
        <#list packages as package>

        <fo:block font-size="14pt" font-weight="bold">Package ${package_index + 1}</fo:block>
        <fo:block><fo:leader/></fo:block>
        <fo:table font-size="12pt">
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonFor}: ${customerName?default("N/A")}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${uiLabelMap.OpentapsPONumber}: ${customerPoNumber?default("N/A")}</fo:block></fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
        <fo:block><fo:leader/></fo:block>

        <fo:block space-after.optimum="10pt" font-size="10pt">
        <fo:table>
            <fo:table-column column-width="150pt"/>
            <fo:table-column column-width="150pt"/>
            <fo:table-column column-width="150pt"/>
            <fo:table-header>
                <fo:table-row font-weight="bold">
                    <fo:table-cell padding="2pt" background-color="#D4D0C8">
                        <fo:block>${uiLabelMap.OpentapsShippingAddress}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8">
                        <fo:block text-align="center">${uiLabelMap.ProductShipmentMethod}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8">
                        <fo:block text-align="right">${uiLabelMap.ProductHandlingInstructions}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell padding="2pt">
                        <fo:block>
                            <@displayAddress postalAddress=destinationPostalAddress />
                        </fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt">
                        <fo:block text-align="center">
                            <#if carrier != "_NA_">
                               ${carrier}
                            </#if>
                            ${shipGroup.shipmentMethodTypeId?default("??")}
                        </fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt">
                        <fo:block text-align="right">${shipment.handlingInstructions?if_exists}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
        </fo:block>

        <fo:block space-after.optimum="10pt" font-size="10pt">
        <fo:table>
            <fo:table-column column-width="223pt"/>
            <fo:table-column column-width="57pt"/>
            <fo:table-column column-width="57pt"/>
            <fo:table-column column-width="57pt"/>
            <fo:table-column column-width="57pt"/>
            <fo:table-header>
                <fo:table-row font-weight="bold">
                    <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductLotId}</fo:block></fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>Requested</fo:block></fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>In this Package</fo:block></fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>Total Shipped</fo:block></fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
                <#list package as line>
                        <#if ((line_index % 2) == 0)>
                            <#assign rowColor = "white">
                        <#else>
                            <#assign rowColor = "#CCCCCC">
                        </#if>

                        <fo:table-row>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <#if line.product?has_content>
                                    <fo:block>${line.product.internalName?default("Internal Name Not Set!")} [${line.product.productId}]</fo:block>
                                <#else/>
                                    <fo:block>${line.getClass().getName()}&nbsp;</fo:block>
                                </#if>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${line.lotId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${line.quantityRequested?default(0)}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${line.quantityInPackage?default(0)}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${line.quantityShipped?default(0)}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                </#list>
            </fo:table-body>
        </fo:table>
        </fo:block>

        <#if shipGroup.giftMessage?has_content>
        <fo:block space-after.optimum="10pt" font-size="10pt">
        <fo:table>
            <fo:table-column column-width="450pt"/>
            <fo:table-body>
                <#if shipGroup.giftMessage?exists >
                    <fo:table-row font-weight="bold">
                        <fo:table-cell>
                            <fo:block>${uiLabelMap.OrderGiftMessage}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row >
                        <fo:table-cell>
                            <fo:block>${shipGroup.giftMessage}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
            </fo:table-body>
        </fo:table>
        </fo:block>
      </#if>


        <#if package_has_next><fo:block break-before="page"/></#if>
        </#list> <#-- packages -->
        <#else>
            <fo:block font-size="14pt">
                ${uiLabelMap.ProductErrorNoPackagesFoundForShipment}
            </fo:block>
        </#if>

        <fo:block id="theEnd"/>  <#-- Put this at end of flow so that footer knows how many pages there are. -->        
    </fo:flow>

    </fo:page-sequence>

</fo:root>
</#escape>
