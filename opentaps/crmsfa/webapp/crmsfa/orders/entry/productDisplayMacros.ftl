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

<#include "renderPrice.ftl">

<#macro renderProductRow product>
    <tr>
      <td>
        <#if product.imageUrl?has_content>
        <div class="smallimage">
          <img src="<@ofbizContentUrl>${product.imageUrl}</@ofbizContentUrl>" height="50" border="1">
        </div>
        </#if>
      </td>
      <td width="100%" style="padding-left: 20px;">
        <#if product.productName?has_content><b>${product.productName}</b><br/></#if>
        <#if product.productDescription?has_content>${product.productDescription}<br/></#if>
        <b>${product.productId}</b> <#if product.productAssocDescription?has_content>(${product.productAssocDescription})</#if>
        <#if product.priceInfo?has_content && product.priceInfo.validPriceFound>
        <br/>
        <@renderPriceForSummary price=product.priceInfo product=product />
        </#if>
      </td>
      <td align="right" nowrap>
        <#if product.isVirtual?default("N") == "Y">
          <a href="<@ofbizUrl>chooseVariantProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CrmChooseVariantProduct}</a>
        <#elseif product.productTypeId == "AGGREGATED">
          <a href="<@ofbizUrl>configureProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CrmConfigureProduct}</a>
        <#else>
          <form method="POST" action="<@ofbizUrl>addOrderItem</@ofbizUrl>">
            <input type="hidden" name="productId" value="${product.productId}">
            <input type="text" name="quantity" class="inputBox" size="3" value="1">
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}">
          </form>
        </#if>
      </td>
    </tr>
  </form>
</#macro>
