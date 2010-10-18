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

<#if associatedProducts?exists && associatedProducts.size() != 0>

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#include "productDisplayMacros.ftl">

<@frameSectionHeader title=uiLabelMap.ProductCrossSell/>

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
