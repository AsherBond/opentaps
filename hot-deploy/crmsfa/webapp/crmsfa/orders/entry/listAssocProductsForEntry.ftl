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

<#if associatedProducts?exists && associatedProducts.size() != 0>

<#include "productDisplayMacros.ftl">

<div class="subSectionHeader">
  <div class="subSectionTitle">Cross-Sell and Upgrades</div>
</div>

<div class="form">
  <table width="70%">
    <#list associatedProducts as product>
      <@renderProductRow product=product />
      <#if product_has_next>
        <tr><td colspan="3"><hr class="sepbar"></td></tr>
      </#if>
    </#list>
  </table>
</div>

</#if>
