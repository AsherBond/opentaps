<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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


<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'><b>${uiLabelMap.CrmOrderSummary}</b></div>
    </div>
    <div class="screenlet-body">
        <#if (shoppingCartSize > 0)>
          <table width="100%" cellpadding="0" cellspacing="2">
            <tr>
              <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.OrderQty}</b></div></td>
              <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.OrderItem}</b></div></td>
              <td valign="bottom" align="right"><div class="tabletext"><b>${uiLabelMap.CommonSubtotal}</b></div></td>
            </tr>
            <#list shoppingCart.items() as cartLine>
              <tr>
                <td valign="top"><div class="tabletext">${cartLine.getQuantity()?string.number}</div></td>
                <td valign="top">
                  <#if cartLine.getProductId()?exists>
                      <#if cartLine.getParentProductId()?exists>
                          <div><a href="<@ofbizUrl>product?product_id=${cartLine.getParentProductId()}</@ofbizUrl>" class="linktext">${cartLine.getName()}</a></div>
                      <#else>
                          <div><a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>" class="linktext" style=${warningStyle?default("")}>${cartLine.getName()}</a></div>
                      </#if>
                  <#else>
                    <div class="tabletext"><b>${cartLine.getItemTypeDescription()?if_exists}</b></div>
                  </#if>
                </td>
                <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency() rounding=2/></div></td>
              </tr>
              <#if cartLine.getReservStart()?exists>
                <tr><td>&nbsp;</td><td colspan="2"><div class="tabletext">(${getLocalizedDate(cartLine.getReservStart, "DATE")}, ${cartLine.getReservLength()} ${uiLabelMap.CommonDays})</div></td></tr>
              </#if>
            </#list>
            <tr>
              <td colspan="3" align="right">
                <div class="tabletext">${uiLabelMap.OrderAdjustments}: <@ofbizCurrency amount=(shoppingCart.getGrandTotal() - shoppingCart.getDisplaySubTotal()) isoCode=shoppingCart.getCurrency() rounding=2/></div>
              </td>
            </tr>
            <tr>
              <td colspan="3" align="right">
                <div class="tabletext"><b>${uiLabelMap.OrderTotal}: <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=shoppingCart.getCurrency() rounding=2/></b></div>
              </td>
            </tr>
          </table>
        <#else>
          <div class="tabletext">${uiLabelMap.CrmOrderNoItems}</div>
        </#if>
    </div>
</div>
