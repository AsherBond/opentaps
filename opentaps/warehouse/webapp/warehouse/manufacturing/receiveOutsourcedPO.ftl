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

<#-- Note this file is a cousin to invoiceSupplies.ftl -->
<div class="subSectionBlock">
  <div class="form">
  <form method="GET" action="<@ofbizUrl>receiveOutsourcedPO</@ofbizUrl>" name="invoiceOrder">
    <span class="tableheadtext">${uiLabelMap.OrderOrderId}: </span>
    <@inputLookup name="orderId" lookup="LookupPurchaseOrder" form="invoiceOrder"/>
    <@inputSubmit title=uiLabelMap.OpentapsFindOrder />
    <@displayError name="orderId"/>
  </form>
  </div>
</div>

<#if orderItems?exists && orderItems.size() != 0>
<form method="post" action="<@ofbizUrl>createOutsourcedInvoice?orderId=${parameters.orderId}</@ofbizUrl>" name="createOutsourcedInvoice">

  <table class="listTable">
      <tr class="listTableHeader">
          <td>${uiLabelMap.CommonDescription}</td>
          <td>${uiLabelMap.ManufacturingProductionRun}</td>
          <td>${uiLabelMap.OrderRemaining}</td>
          <td align="right">${uiLabelMap.OrderUnitPrice}</td>
          <td align="right">${uiLabelMap.OrderRemainingSubTotal}</td>
          <td align="right">${uiLabelMap.OpentapsQtyToReceive}</td>
          <td></td>
      </tr>

      <#assign itemsToReceive = 0/>
      <#list orderItems as orderItem>
          <@inputHidden name="orderId" value=orderItem.orderId index=orderItem_index />
          <@inputHidden name="orderItemSeqId" value=orderItem.orderItemSeqId index=orderItem_index />
          <@inputHidden name="workEffortId" value=orderItem.workEffortId index=orderItem_index />
          <@inputHiddenRowSubmit index=orderItem_index />

          <tr class="${tableRowClass(orderItem_index)}">
              <td>${orderItem.itemDescription?default(orderItem.productId?default(orderItem.comments?default(orderItem.orderItemSeqId)))}</td>
              <@displayLinkCell href="ShowProductionRun?productionRunId=${orderItem.productionRunId}" text=orderItem.productionRunId/>
              <td>${orderItem.quantity}</td>
              <@displayCurrencyCell amount=orderItem.unitPrice currencyUomId=order.currencyUom />
              <@displayCurrencyCell amount=(orderItem.unitPrice * orderItem.quantity) currencyUomId=order.currencyUom />
              <#if (orderItem.quantity > 0)>
                 <#assign itemsToReceive = itemsToReceive + 1/>
                 <td align="right">
                  <@inputText name="quantity" index=orderItem_index size=6 />
                 </td>
              </#if>
              <td><@displayError name="quantity" index=orderItem_index /></td>
          </tr>
      </#list>

      <#if (itemsToReceive > 0)>
        <tr>
          <td colspan=5"></td>
          <@inputSubmitCell title=uiLabelMap.CommonReceive blockClass="textright" />
          <td></td>
        </tr>
      </#if>
  </table>

</form>
<#elseif orderItems?exists && orderItems.size() == 0>
  <div class="form"><span class="tableheadtext">${uiLabelMap.OpentapsNothingToInvoice}</span></div>
</#if>
