<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
-->

<#-- NOTE: these forms don not use rowSubmit to enter multi form values because the parsing function is custom -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.CommonSearchResultfor} ${parameters.productId}</div>
  <div class="subMenuBar"><@displayLink href="createOrderMainScreen" text="${uiLabelMap.OpentapsOrderReturnToOrder}"/></div>
</div>

<@paginate name="matchingSupplierProducts" list=matchingSupplierProducts rememberPage=false>
<#noparse>

<form name="findMatchingSupplierProducts" method="post" action="<@ofbizUrl>BulkAddProducts</@ofbizUrl>">

  <table class="listTable">
    <tr class="listTableHeader">
      <@headerCell title=uiLabelMap.ProductProductId orderBy="productId" />
      <@headerCell title=uiLabelMap.ProductGoodIdentification orderBy="idValue" />
      <@headerCell title=uiLabelMap.ProductSupplierProductId orderBy="supplierProductId" />
      <@headerCell title=uiLabelMap.ProductLastPrice orderBy="lastPrice,productId" blockClass="textright" />
      <@headerCell title=uiLabelMap.FormFieldTitle_minimumOrderQuantity orderBy="minimumOrderQuantity,partyId" blockClass="textright" />
      <td align="right">${uiLabelMap.CommonQuantity}</td>
      <td>${uiLabelMap.OrderDesiredDeliveryDate}</td>
    </tr>

    <#list pageRows as supplierProduct>

      <@inputHidden name="productId" value=supplierProduct.productId index=supplierProduct_index />
      <#if supplierProduct.itemType?has_content>
        <@inputHidden name="itemType" value=supplierProduct.itemType index=supplierProduct_index />
      </#if>

      <tr class="${tableRowClass(supplierProduct_index)}">
        <@displayCell text=supplierProduct.productId />
        <@displayCell text=supplierProduct.idValue />
        <@displayCell text=supplierProduct.supplierProductId />
        <@displayCurrencyCell amount=supplierProduct.lastPrice currencyUomId=supplierProduct.currencyUomId />
        <@displayCell text=supplierProduct.minimumOrderQuantity  blockClass="textright" />
        <td align="right"><@inputText name="quantity" size=5 index=supplierProduct_index /></td>
        <@inputDateCell name="itemDesiredDeliveryDate" form="findMatchingSupplierProducts" size=22 index=supplierProduct_index />
      </tr>
    </#list>

    <@inputHidden name="_rowCount" value=pageRows?size />

    <tr>
      <td colspan="5"></td>
      <td align="right"><@inputSubmit title=uiLabelMap.OrderAddToOrder /></td>
      <td></td>
    </tr>
  </table>
  <div align="right"><@paginationNavContext /></div>

</form>

</#noparse>
</@paginate>
