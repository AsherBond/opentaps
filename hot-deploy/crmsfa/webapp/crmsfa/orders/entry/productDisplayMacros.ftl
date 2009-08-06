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
