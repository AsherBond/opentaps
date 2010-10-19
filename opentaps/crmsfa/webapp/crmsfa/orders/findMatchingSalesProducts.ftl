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

<#-- NOTE: these forms don not use rowSubmit to enter multi form values because the parsing function is custom -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.CommonSearchResultfor} ${parameters.productId}</div>
  <div class="subMenuBar"><@displayLink href="createOrderMainScreen" text="${uiLabelMap.OpentapsOrderReturnToOrder}" class="buttontext"/></div>
</div>

<@paginate name="matchingSalesProducts" list=matchingSalesProducts rememberPage=false>
<#noparse>

<form name="findMatchingSalesProducts" method="post" action="<@ofbizUrl>BulkAddProducts</@ofbizUrl>">

  <table class="listTable">
    <tr class="listTableHeader">
      <@headerCell title=uiLabelMap.ProductProductId orderBy="productId" />
      <@headerCell title=uiLabelMap.ProductGoodIdentification orderBy="idValue" />
      <@headerCell title=uiLabelMap.ProductBrandName orderBy="brandName" />
      <@headerCell title=uiLabelMap.ProductInternalName orderBy="internalName" />
      <td align="right">${uiLabelMap.CommonQuantity}</td>
      <td>${uiLabelMap.OrderDesiredDeliveryDate}</td>
    </tr>

    <#list pageRows as product>

      <#if product.isSurvey>
      <tr class="${tableRowClass(product_index)}">
        <@displayCell text=product.productId />
        <@displayCell text=product.idValue />
        <@displayCell text=product.brandName />
        <@displayCell text=product.internalName />
        <td colspan="2">
          This product requires a survey response and cannot be bulk added.
        </td>
      </tr>
      <#else>

      <@inputHidden name="productId" value=product.productId index=product_index />
      <#if product.itemType?has_content>
        <@inputHidden name="itemType" value=product.itemType index=product_index />
      </#if>

      <tr class="${tableRowClass(product_index)}">
        <@displayCell text=product.productId />
        <@displayCell text=product.idValue />
        <@displayCell text=product.brandName />
        <@displayCell text=product.internalName />
        <td align="right"><@inputText name="quantity" size=5 index=product_index /></td>
        <@inputDateCell name="itemDesiredDeliveryDate" form="findMatchingSalesProducts" size=22 index=product_index />
      </tr>

      </#if>
    </#list>

    <@inputHidden name="_rowCount" value=pageRows?size />

    <tr>
      <td colspan="4"></td>
      <td align="right"><@inputSubmit title=uiLabelMap.OrderAddToOrder /></td>
      <td></td>
    </tr>
  </table>
  <div align="right"><@paginationNavContext /></div>

</form>
        
</#noparse>
</@paginate>
