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
<#macro displayAddressHeader postalAddress>
   <#if postalAddress.toName?has_content><fo:block>${postalAddress.toName?if_exists}</fo:block></#if>
   <#if postalAddress.attnName?has_content><fo:block>${postalAddress.attnName?if_exists}</fo:block></#if>
   <fo:block>${postalAddress.address1?if_exists}</fo:block>
   <#if postalAddress.address2?has_content><fo:block>${postalAddress.address2?if_exists}</fo:block></#if>
   <fo:block>${postalAddress.city?if_exists}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if><#if postalAddress.postalCode?has_content>${postalAddress.postalCode}</#if></fo:block>
   <#if postalAddress.countryGeoId?default("USA") != "USA">
     <fo:block>${postalAddress.countryGeoId}</fo:block>
   </#if>
</#macro>
       <fo:table border-spacing="3pt" font-size="9pt">
         <fo:table-column column-width="3.75in"/>
         <fo:table-column column-width="3.75in"/>
         <fo:table-body>
           <fo:table-row>

             <#-- For POs, the supplier contact mech is stored separately. -->
             <#if order.isPurchaseOrder()>
               <fo:table-cell>
                 <fo:block font-weight="bold">${uiLabelMap.OpentapsPurchasedFrom}:</fo:block>
                 <#if supplierAddress?has_content>
                   <@displayAddressHeader postalAddress=supplierAddress />
                 <#else>
                   <fo:block>${uiLabelMap.PartyNoContactInformation}</fo:block>
                 </#if>
               </fo:table-cell>
             </#if>

             <#-- list all postal addresses of the order.  there should be just a billing and a shipping here. -->
             <#list order.orderContactMeches as orderContactMech>
               <#assign contactMech = orderContactMech.contactMech/>
               <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
                 <#assign contactMechPurpose = orderContactMech.contactMechPurposeType/>
                 <#assign postalAddress = contactMech.postalAddress/>
                 <fo:table-cell>
                   <fo:block font-weight="bold">${contactMechPurpose.get("description",locale)}:</fo:block>
                   <#if postalAddress?has_content>
                     <@displayAddressHeader postalAddress=postalAddress />
                   <#else>
                     <fo:block>${uiLabelMap.PartyNoContactInformation}</fo:block>
                   </#if>
                 </fo:table-cell>
               </#if>
             </#list>
            </fo:table-row>
         </fo:table-body>
       </fo:table>

       <fo:block white-space-collapse="false"> </fo:block> 

       <fo:table border-spacing="3pt" font-size="9pt">
          <fo:table-column column-width="1.75in"/>
          <fo:table-column column-width="4.25in"/>
          <fo:table-body>
        
        <#-- shipping method.  currently not shown for PO's because we are not recording a shipping method for PO's in order entry -->
           <#if orderHeader.getString("orderTypeId") == "SALES_ORDER">
            <fo:table-row>
               <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OpentapsShipVia}:</fo:block></fo:table-cell>
                  <fo:table-cell>
                 <#if shipGroups?has_content>
                   <#list shipGroups as shipGroup>
                   <#-- TODO: List all full details of each ship group here -->
                        <fo:block>
                        <#assign carrierName = shipGroup.get("carrierName")?if_exists/>
                        <#assign shipmentMethodType = shipGroup.get("shipmentMethodType")?if_exists/>
                        ${carrierName?if_exists} <#if shipmentMethodType?exists>${shipmentMethodType.description?if_exists}</#if>
                     </fo:block>
                   </#list>
                  </#if>
               </fo:table-cell>
             </fo:table-row>
           </#if>
       <#-- order terms information -->
             <#if orderTerms?has_content>
               <fo:table-row>
                 <fo:table-cell>
                   <fo:block font-weight="bold">${uiLabelMap.OrderOrderTerms}: </fo:block>
                 </fo:table-cell>
               </fo:table-row>
               <fo:table-row>
                 <fo:table-cell>
                   <fo:table>
                     <fo:table-column column-width="2in"/>
                     <fo:table-column column-width="0.5in"/>
                     <fo:table-column column-width="0.5in"/>
                     <fo:table-column column-width="4.5in"/>
                     <fo:table-body>
                       <#list orderTerms as orderTerm>
                         <fo:table-row>
                           <fo:table-cell white-space-collapse="false"><fo:block>${orderTerm.getRelatedOne("TermType").get("description",locale)}</fo:block></fo:table-cell>
                           <fo:table-cell white-space-collapse="false"><fo:block>${orderTerm.termValue?default("")}</fo:block></fo:table-cell>
                           <fo:table-cell white-space-collapse="false"><fo:block>${orderTerm.termDays?default("")}</fo:block></fo:table-cell>
                           <fo:table-cell white-space-collapse="false"><fo:block>${orderTerm.textValue?default("")}</fo:block></fo:table-cell>
                         </fo:table-row>
                       </#list>
                     </fo:table-body>
                   </fo:table>
                 </fo:table-cell>
               </fo:table-row>
             </#if>
          </fo:table-body>
       </fo:table>

<fo:block space-after="10pt"/>
</#escape>
