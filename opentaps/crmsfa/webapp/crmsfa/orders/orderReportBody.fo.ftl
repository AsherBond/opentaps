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

<#escape x as x?xml>
     <#if order?has_content>
        <fo:table border-spacing="3pt" font-size="8pt">

       <fo:table-column column-width="3.5in"/>
       <fo:table-column column-width="1in"/>
       <fo:table-column column-width="1in"/>
       <fo:table-column column-width="1in"/>
  
       <fo:table-header>
           <fo:table-row>
               <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
               <fo:table-cell text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderQuantity}</fo:block></fo:table-cell>
               <fo:table-cell text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderUnitPrice}</fo:block></fo:table-cell>
               <fo:table-cell text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderSubTotal}</fo:block></fo:table-cell>
           </fo:table-row>
       </fo:table-header>
        
       <fo:table-body>
         <#list order.items as item>
           <fo:table-row>
             <fo:table-cell>
               <fo:block>
                 <#if item.productId?exists>
                   ${item.productId?default("N/A")} - ${item.itemDescription?if_exists}
                 <#elseif orderItemType?exists>
                   ${item.type.get("description",locale)} - ${item.itemDescription?if_exists}
                 <#else>
                   ${item.itemDescription?if_exists}
                 </#if>
               </fo:block>
             </fo:table-cell>
             <fo:table-cell text-align="right"><fo:block>${item.orderedQuantity}</fo:block></fo:table-cell>            
             <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=item.unitPrice isoCode=order.currencyUom/></fo:block></fo:table-cell>
             <fo:table-cell text-align="right"><fo:block>
                 <#if !item.isCancelled()>
                   <@ofbizCurrency amount=item.subTotal isoCode=order.currencyUom/>
                 <#else/>
                   <@ofbizCurrency amount=0.00 isoCode=order.currencyUom/>
                 </#if>
             </fo:block></fo:table-cell>
           </fo:table-row>
           <#if item.otherAdjustmentsAmount != 0>
             <fo:table-row>
               <fo:table-cell number-columns-spanned="2"><fo:block><fo:inline font-style="italic">${uiLabelMap.OrderAdjustments}</fo:inline>: <@ofbizCurrency amount=item.otherAdjustmentsAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
             </fo:table-row>
           </#if>
         </#list>

         <#-- order items and tax sub totals -->
         <fo:table-row>
           <fo:table-cell number-columns-spanned="3" text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderItemsSubTotal}</fo:block></fo:table-cell>
           <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.itemsSubTotal isoCode=order.currencyUom/></fo:block></fo:table-cell>
         </fo:table-row>
         <#if order.taxAmount != 0>
           <fo:table-row>
             <fo:table-cell number-columns-spanned="3" text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderTotalSalesTax}</fo:block></fo:table-cell>
             <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.taxAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
           </fo:table-row>
         </#if>

         <#macro displayAdjustment orderHeaderAdjustment>
           <#assign adjustmentAmount = orderHeaderAdjustment.calculateAdjustment(order)/>
           <#if adjustmentAmount != 0>
             <fo:table-row>
               <fo:table-cell number-columns-spanned="3" text-align="right"><fo:block font-weight="bold"><#if orderHeaderAdjustment.description?has_content>${orderHeaderAdjustment.description?if_exists}<#else>${orderHeaderAdjustment.type.get("description",locale)} </#if> </fo:block></fo:table-cell>
               <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=adjustmentAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
             </fo:table-row>
           </#if>
         </#macro>

         <#-- shipping and sub totals -->
         <#list order.shippingAdjustments as orderHeaderAdjustment>
           <@displayAdjustment orderHeaderAdjustment=orderHeaderAdjustment/>
         </#list>
         <#if order.shippingAmount != 0>
           <fo:table-row>
             <fo:table-cell number-columns-spanned="3" text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderTotalShippingAndHandling}</fo:block></fo:table-cell>
             <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.shippingAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
           </fo:table-row>
         </#if>

          <#-- other adjustments such as promotions -->
          <#list order.nonShippingAdjustments as orderHeaderAdjustment>
             <@displayAdjustment orderHeaderAdjustment=orderHeaderAdjustment/>
          </#list>
          <#if order.otherAdjustmentsAmount != 0>
            <fo:table-row>
              <fo:table-cell number-columns-spanned="3" text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderTotalOtherOrderAdjustments}</fo:block></fo:table-cell>
              <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.otherAdjustmentsAmount isoCode=order.currencyUom/></fo:block></fo:table-cell>
            </fo:table-row>
          </#if>
                  
          <#-- order grand total -->
          <#if order.total != 0>
            <fo:table-row>
              <fo:table-cell></fo:table-cell>
              <fo:table-cell number-columns-spanned="2" text-align="right"><fo:block font-weight="bold">${uiLabelMap.OrderTotalDue}</fo:block></fo:table-cell>
              <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=order.total isoCode=order.currencyUom/></fo:block></fo:table-cell>
            </fo:table-row>
          </#if>

           <#-- notes -->
           <#if order.notes?has_content>
             <fo:table-row >
               <fo:table-cell number-columns-spanned="3">
                 <fo:block font-weight="bold">${uiLabelMap.OrderNotes}</fo:block>
               </fo:table-cell>    
             </fo:table-row>    
             <#list order.notes as note>
               <#if (note.internalNote?has_content) && (note.internalNote != "Y")>
                 <fo:table-row>
                   <fo:table-cell number-columns-spanned="6">
                     <fo:block><fo:leader leader-length="19cm" leader-pattern="rule" /></fo:block>    
                   </fo:table-cell>
                 </fo:table-row>
                 <fo:table-row>
                   <fo:table-cell number-columns-spanned="1">
                     <fo:block>${note.noteInfo?if_exists}</fo:block>    
                   </fo:table-cell>
                   <fo:table-cell number-columns-spanned="2">
                     <#assign notePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", note.noteParty, "compareDate", note.noteDateTime, "lastNameFirst", "Y", "userLogin", userLogin))/>
                     <fo:block>${uiLabelMap.CommonBy}: ${notePartyNameResult.fullName?default("${uiLabelMap.OrderPartyNameNotFound}")}</fo:block>
                   </fo:table-cell>
                   <fo:table-cell number-columns-spanned="1">
                     <fo:block>${uiLabelMap.CommonAt}: ${note.noteDateTime?string?if_exists}</fo:block>    
                   </fo:table-cell>
                 </fo:table-row>
               </#if>                  
             </#list>
           </#if>
         </fo:table-body>
       </fo:table>    
     </#if>
</#escape>
