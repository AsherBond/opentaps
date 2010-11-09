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

<#-- This file has been modified by Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>
<fo:block space-before="40pt" space-after="40pt">
</fo:block>
<#escape x as x?xml>

<#list shipGroups as shipGroup>
  <#assign data = groupData.get(shipGroup.shipGroupSeqId)>

  <#-- print the order ID, ship group, and their bar codes -->

  <fo:table table-layout="fixed" space-after.optimum="10pt">
    <fo:table-column/>
    <fo:table-column/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell>
          <fo:block font-size="14pt">${uiLabelMap.OrderOrder} #${shipGroup.orderId}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block text-align="right">
            <fo:instream-foreign-object>
              <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="${shipGroup.orderId}">
                <barcode:code39><barcode:height>8mm</barcode:height></barcode:code39>
              </barcode:barcode>
            </fo:instream-foreign-object>
          </fo:block>
        </fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell>
          <fo:block font-size="14pt">${uiLabelMap.OrderShipGroup} #${shipGroup.shipGroupSeqId}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block text-align="right">
            <fo:instream-foreign-object>
              <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="${shipGroup.shipGroupSeqId}">
                <barcode:code39><barcode:height>8mm</barcode:height></barcode:code39>
              </barcode:barcode>
            </fo:instream-foreign-object>
          </fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-body>
  </fo:table>

  <#-- print the address, carrier, and shipment dates -->

  <fo:table table-layout="fixed" space-after.optimum="10pt">
    <fo:table-column column-width="proportional-column-width(4)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(2)"/>
    <fo:table-body>
      <fo:table-row>
        <#assign rowsToSpan = 2/>
        <#if shipGroup.shipAfterDate?has_content>
          <#assign rowsToSpan = rowsToSpan + 1/>
        </#if>
        <#if shipGroup.shipByDate?has_content>
          <#assign rowsToSpan = rowsToSpan + 1/>
        </#if>
        <#if shipGroup.shippingInstructions?has_content>
          <#assign rowsToSpan = rowsToSpan + 1/>
        </#if>
        <fo:table-cell number-rows-spanned="${rowsToSpan}">
        <#if data.address?exists>
          <#assign address = data.address>
          <fo:block>${uiLabelMap.CommonTo}: ${address.toName?if_exists}</fo:block>
          <#if address.attnName?has_content>
          <fo:block>${uiLabelMap.CommonAttn}: ${address.attnName?if_exists}</fo:block>
          </#if>
          <fo:block>${address.address1?if_exists}</fo:block>
          <fo:block>${address.address2?if_exists}</fo:block>
          <fo:block>
            ${address.city?if_exists}<#if address.stateProvinceGeoId?has_content>, ${address.stateProvinceGeoId}</#if>
            ${address.postalCode?if_exists} ${address.countryGeoId?if_exists}
          </fo:block>

          <#if data.phoneNumber?exists>
            <fo:block>${uiLabelMap.OpentapsPhoneNumber}: ${data.phoneNumber.countryCode?if_exists} (${data.phoneNumber.areaCode}) ${data.phoneNumber.contactNumber}</fo:block>
          </#if>
        <#else>
          <fo:block>${uiLabelMap.CommonTo}: ${uiLabelMap.OpentapsUnknown}</fo:block>
        </#if>
        </fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OpentapsShipVia}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${data.carrierShipmentMethod.partyId} ${data.shipmentMethodType.description}</fo:block></fo:table-cell>
      </fo:table-row>
      <#if shipGroup.shipAfterDate?has_content>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OpentapsShipAfter}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${shipGroup.shipAfterDate?default("N/A")}</fo:block></fo:table-cell>
      </fo:table-row>
      </#if>
      <#if shipGroup.shipByDate?has_content>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OpentapsShipBefore}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${shipGroup.shipByDate?default("N/A")}</fo:block></fo:table-cell>
      </fo:table-row>
      </#if>
      <#if shipGroup.shippingInstructions?has_content>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderInstructions}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${shipGroup.shippingInstructions}</fo:block></fo:table-cell>
      </fo:table-row>
      </#if>       
    </fo:table-body>
  </fo:table>
  
  <#if data.orderNotes?default([])?has_content>
    <fo:block space-after.optimum="10pt">
      ${uiLabelMap.OrderNotes}
    </fo:block>
    <fo:block space-after.optimum="15pt">
      <#list data.orderNotes?default([]) as orderNote>
        <fo:block>
          ${orderNote.noteInfo?if_exists}
        </fo:block>
        <fo:block text-align="right">
          - ${orderNote.notePartyName?if_exists}, <@displayDateFO date=orderNote.noteDateTime format="DATE_TIME"/>    
        </fo:block>
      </#list>
    </fo:block>
  </#if>
  
  <#assign lines = data.lines>
  <fo:table table-layout="fixed">
    <fo:table-column column-width="proportional-column-width(2)"/>
    <fo:table-column column-width="proportional-column-width(3)"/>
    <fo:table-column column-width="proportional-column-width(2)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>

    <fo:table-header>
      <fo:table-row font-weight="bold">
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.ProductProduct}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.CommonDescription}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.ProductLotId}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.OrderOrdered}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.OrderQuantityShipped}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.ProductOpenQuantity}</fo:block>
        </fo:table-cell>
      </fo:table-row>        
    </fo:table-header>
    <fo:table-body>

      <#list lines as line>
        <#if ((line_index % 2) == 0)>
          <#assign rowColor = "white">
        <#else>
          <#assign rowColor = "#CCCCCC">
        </#if>

      <fo:table-row>
        <fo:table-cell background-color="${rowColor}">
          <fo:block>${line.product.productId}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block>${line.orderItem.itemDescription?if_exists}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block>${line.lotId?if_exists}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityInGroup?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityShipped?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityOpen?default(0)}</fo:block>
        </fo:table-cell>

      </fo:table-row>

      <#list line.expandedList?if_exists as expandedLine>
      <fo:table-row>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block margin-left="20pt">${expandedLine.product.productId}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block margin-left="20pt">${expandedLine.product.internalName}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowcolor}" font-style="italic">
          <fo:block text-align="right"></fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityInGroup?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityShipped?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityOpen?default(0)}</fo:block>
        </fo:table-cell>
      </fo:table-row>
      </#list>

      </#list>

  </fo:table-body>
</fo:table>

  <#if shipGroup_has_next><fo:block break-before="page"/></#if>
</#list>

</#escape>
