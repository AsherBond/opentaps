<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if allRoutingProductLinks?has_content>
  <table class="listTable">
    <tr class="listTableHeader">
      <@displayCell text=uiLabelMap.ProductProduct />
      <@displayCell text=uiLabelMap.CommonFromDate />
      <@displayCell text=uiLabelMap.CommonThruDate />
      <@displayCell text=uiLabelMap.ManufacturingQuantity />
      <@displayCell text=uiLabelMap.PurchMinQuantity />
      <@displayCell text=uiLabelMap.PurchMaxQuantity />
      <@displayCell text=uiLabelMap.FormFieldTitle_estimatedCost />
      <td>&nbsp;</td>
    </tr>
    <#list allRoutingProductLinks as productLink>
      <#assign listToProduct = productLink.getRelatedOneCache("Product")/>
      <tr class="${tableRowClass(productLink_index)}">
        <@displayLinkCell text="[${productLink.productId}] ${listToProduct.internalName}" href="EditProductBom?productId=${productLink.productId}&productAssocTypeId=MANUF_COMPONENT" />
        <@displayCell text=productLink.fromDate />
        <@displayCell text=productLink.thruDate />
        <@displayCell text=productLink.estimatedQuantity />
        <@displayCell text=productLink.minQuantity />
        <@displayCell text=productLink.maxQuantity />
        <@displayCell text=productLink.estimatedCost />
        <@displayLinkCell text=uiLabelMap.CommonEdit href="EditRoutingProductLink?workEffortId=${productLink.workEffortId}&productId=${productLink.productId}&fromDate=${productLink.fromDate}&workEffortGoodStdTypeId=${productLink.workEffortGoodStdTypeId}" class="buttontext"/>
        <@displayLinkCell text=uiLabelMap.CommonDelete href="removeRoutingProductLink?workEffortId=${productLink.workEffortId}&productId=${productLink.productId}&fromDate=${productLink.fromDate}&workEffortGoodStdTypeId=${productLink.workEffortGoodStdTypeId}" class="buttontext"/>
      </tr>
    </#list>
  </table>
</#if>
