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
"License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#-- This file has been modified by Open Source Strategies, Inc. -->
        
<#-- The macros in this file are used to render the price information for a product. -->

<#--
  This macro is used to render a price in one line from a price map, which are the OUT parameters of calculateProductPrice or calculatePurchasePrice.
  Use this for compact price information, such as found in the cross-sell and upgrade list of products in order entry.
  This code originally came from productsummary.ftl in the Apache OFBiz order application.
-->
<#macro renderPriceForSummary price product>
  <#if price.competitivePrice?exists && price.price?exists && price.price?double < price.competitivePrice?double>
    ${uiLabelMap.ProductCompareAtPrice}: <span class='basePrice'><@ofbizCurrency amount=price.competitivePrice isoCode=price.currencyUsed/></span>
  </#if>
  <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
    ${uiLabelMap.ProductListPrice}: <span class="basePrice"><@ofbizCurrency amount=price.listPrice isoCode=price.currencyUsed/></span>
  </#if>
  <b>
  <#if price.isSale?exists && price.isSale>
    <span class="salePrice">${uiLabelMap.EcommerceOnSale}!</span>
    <#assign priceStyle = "salePrice">
  <#else>
    <#assign priceStyle = "regularPrice">
  </#if>
  <#if (price.price?default(0) > 0 && product.requireAmount?default("N") == "N")>
    ${uiLabelMap.EcommerceYourPrice}: <#if "Y" = product.isVirtual?if_exists> ${uiLabelMap.CommonFrom} </#if><span class="${priceStyle}"><@ofbizCurrency amount=price.price isoCode=price.currencyUsed/></span>
  </#if>
  </b>
  <#if price.listPrice?exists && price.price?exists && price.price?double < price.listPrice?double>
    <#assign priceSaved = price.listPrice?double - price.price?double>
    <#assign percentSaved = (priceSaved?double / price.listPrice?double) * 100>
    ${uiLabelMap.EcommerceSave}: <span class="basePrice"><@ofbizCurrency amount=priceSaved isoCode=price.currencyUsed/> (${percentSaved?int}%)</span>
  </#if>
</#macro>
