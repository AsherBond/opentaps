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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if shoppingCart.size() != 0>
  <#assign finalizeLink = "<a class='subMenuButton' href='finalizeOrder?finalizeMode=init'>${uiLabelMap.OpentapsFinalizeOrder}</a>" />
</#if>

<div id="sectionHeaderTitle_order" class="sectionHeader sectionHeaderTitle">
  <span style="float:left;">${uiLabelMap.OrderOrders}</span>
  <div class="subMenuBar" style="float:right; margin:0.4em">${finalizeLink?if_exists}</div>
</div>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.OrderCreateOrder}</div>
  <div class="subMenuBar"><a class="toggleButton" href="createOrderMainScreen?&amp;useGwt=Y">${uiLabelMap.OpentapsGridView}</a><span class="toggleButtonDisabled">${uiLabelMap.OpentapsFullView}</span></div>
</div>

<form name="addOrderItemForm" method="POST" action="<@ofbizUrl>addOrderItem</@ofbizUrl>">

  <table class="fourColumnForm">
    <tr>
      <@displayTitleCell title=uiLabelMap.ProductProductId titleClass="requiredField"/>
      <@inputAutoCompleteProductCell name="productId" errorField="productId" />
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
  <input type="button" value="${uiLabelMap.OrderAddToOrder}" class="smallSubmit" onclick="javascript:opentaps.checkSupplierProduct(this, document.getElementById('productId').value, '${shoppingCart.partyId?if_exists}', '${shoppingCart.getCurrency()?if_exists}', document.getElementById('quantity').value, '${uiLabelMap.PurchOrderConfirmNotExistSupplierProduct}')" />
  </td>
  </tr>    
  </table>

</form>
