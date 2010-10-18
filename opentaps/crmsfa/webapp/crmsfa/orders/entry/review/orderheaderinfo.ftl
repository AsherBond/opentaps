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
        
<div class="screenlet">
    <div class="screenlet-body">
         <table width="100%" border="0" cellpadding="1">
           <#-- order name -->
           <#if (orderName?exists)>
             <tr>
               <td align="right" valign="top" width="15%">
                 <span class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderName}</b> </span>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%" class="tabletext">
                 ${orderName}
               </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
          <#-- order for party -->
           <#if (orderForParty?exists)>
             <tr>
               <td align="right" valign="top" width="15%">
                 <span class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderFor}</b> </span>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%" class="tabletext">
               ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(orderForParty, false)} [${orderForParty.partyId}]
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
          <#if customerPoNumber?exists>
             <tr>
               <td align="right" valign="top" width="15%">
                 <span class="tabletext">&nbsp;<b>${uiLabelMap.OpentapsPONumber}</b> </span>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%" class="tabletext">${customerPoNumber}</td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
            <#if orderTerms?has_content>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderTerms}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                   <table>
                     <tr>
                       <td width="35%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                       <td width="10%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                       <td width="10%"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
                       <td width="45%"><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
                     </tr>
                     <tr><td colspan="4"><hr class="sepbar"/></td></tr>
                     <#assign index=0/>
                     <#list orderTerms as orderTerm>
                       <tr>
                         <td width="35%"><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description",locale)}</div></td>
                         <td width="10%"><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
                         <td width="10%"><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
                         <td width="45%"><div class="tabletext">${orderTerm.description?default("")}</div></td>
                       </tr>
                       <#if orderTerms.size()&lt;index>
                         <tr><td colspan="4"><hr class="sepbar"/></td></tr>
                       </#if>
                       <#assign index=index+1/>
                     </#list>
                </table>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
          </#if>
            <#-- tracking number -->
            <#if trackingNumber?has_content>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.FacilityTrackingNumber}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                  <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
                  <div class="tabletext">${trackingNumber}</div>
                </td>
              </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            </#if>

            <#-- splitting preference -->
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSplittingPreference}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if maySplit?default("N") == "N">${uiLabelMap.FacilityWaitEntireOrderReady}</#if>
                  <#if maySplit?default("Y") == "Y">${uiLabelMap.FacilityShipAvailable}</#if>
                </div>
              </td>
            </tr>
            <#-- shipping instructions -->
            <#if shippingInstructions?has_content>
              <tr><td colspan="7"><hr class="sepbar"/></td></tr>
              <tr>
                <td align="right" valign="top" width="15%">
                  <div class="tabletext">&nbsp;<b>${uiLabelMap.OpentapsInstructions}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                      <div class="tabletext">${shippingInstructions}</div>
                    </td>
              </tr>
            </#if>
            <#-- gift settings -->
                <#if isGift?default("N") == "Y">
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                  <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGift}?</b></div>
              </td>
                  <td width="5">&nbsp;</td>
                  <td align="left" valign="top" width="80%">
                    <div class="tabletext">
                      <#if isGift?default("N") == "Y">${uiLabelMap.OrderThisOrderGift}</#if>
                </div>
              </td>
            </tr>
                  <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                <#if giftMessage?has_content>
              <tr>
                    <td align="right" valign="top" width="15%">
                      <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderGiftMessage}</b></div>
                </td>
                    <td width="5">&nbsp;</td>
                    <td align="left" valign="top" width="80%">
                      <div class="tabletext">${giftMessage}</div>
                </td>
              </tr>
                   <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            </#if>
          </#if>
            <#if shipAfterDate?has_content>
             <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipAfterDate}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext">${getLocalizedDate(shipAfterDate, "DATE")}</div>
                </td>
            </tr>
            </#if>
            <#if shipBeforeDate?has_content>
            <tr>
               <td align="right" valign="top" width="15%">
                   <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderShipBeforeDate}</b></div>
               </td>
               <td width="5">&nbsp;</td>
               <td align="left" valign="top" width="80%">
                   <div class="tabletext">${getLocalizedDate(shipBeforeDate, "DATE")}</div>
               </td>
             </tr>
           </#if>
        </table>
    </div>
</div>
