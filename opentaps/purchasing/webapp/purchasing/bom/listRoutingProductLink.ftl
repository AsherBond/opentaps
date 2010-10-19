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
        <@form name="removeRoutingProductLinkForm_${productLink_index}" url="removeRoutingProductLink" workEffortId="${productLink.workEffortId}" productId="${productLink.productId}" fromDate="${productLink.fromDate}" workEffortGoodStdTypeId="${productLink.workEffortGoodStdTypeId}"/>
        <td>
        <@submitFormLink form="removeRoutingProductLinkForm_${productLink_index}" text="${uiLabelMap.CommonDelete}" class="buttontext"/>
        </td>
      </tr>
    </#list>
  </table>
</#if>
