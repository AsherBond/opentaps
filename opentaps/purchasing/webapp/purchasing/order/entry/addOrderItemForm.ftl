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

<#if shoppingCart.size() != 0>
  <#assign finalizeLink = "<a class='subMenuButton' href='finalizeOrder?finalizeMode=init'>${uiLabelMap.OpentapsFinalizeOrder}</a>" />
</#if>

<@frameSectionTitleBar title=uiLabelMap.OrderOrders titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_order" extra=finalizeLink! />

<#assign createOrderExtraButtons>
<a class="toggleButton" href="createOrderMainScreen?&amp;useGwt=Y">${uiLabelMap.OpentapsGridView}</a><span class="toggleButtonDisabled">${uiLabelMap.OpentapsFullView}</span>
</#assign>

<@frameSectionHeader title=uiLabelMap.OrderCreateOrder extra=createOrderExtraButtons />
<form name="addOrderItemForm" method="POST" action="<@ofbizUrl>addOrderItem</@ofbizUrl>">

  <table class="fourColumnForm">
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductProductId titleClass="requiredField"/>
      <@inputAutoCompleteProductCell name="productId" errorField="productId" url="getAutoCompleteProductNoVirtual"/>
      <td rowspan="4" valign="top">
        <table>
          <#if tagTypes?has_content>
            <@accountingTagsSelectRows tags=tagTypes />
          </#if>
        </table>
      </td>
    </tr>
    <@inputTextRow title=uiLabelMap.CommonQuantity titleClass="requiredField" name="quantity" size=10 default="1" errorField="quantity"  />
    <@inputDateRow title=uiLabelMap.OrderShipBeforeDate name="shipBeforeDate" errorField="shipBeforeDate" />
    <@inputTextareaRow title=uiLabelMap.CommonComment name="comments" cols=40/>
  <tr>
    <td>&nbsp;</td>
    <td>
  <input type="button" value="${uiLabelMap.OrderAddToOrder}" class="smallSubmit" onclick="javascript:opentaps.checkSupplierProduct(this, document.getElementById('productId').value, '${shoppingCart.partyId?if_exists}', '${shoppingCart.getCurrency()?if_exists}', document.getElementById('quantity').value, '${uiLabelMap.PurchOrderConfirmNotExistSupplierProduct}', true)" />
  </td>
  </tr>
  </table>

</form>
