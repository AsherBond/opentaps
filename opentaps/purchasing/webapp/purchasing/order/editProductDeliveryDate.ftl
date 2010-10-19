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
<@import location="component://opentaps-common/webapp/common/includes/lib/flexAreaSearchSwitchMacros.ftl"/>
<#assign searchChoices = {"order":uiLabelMap.OrderPurchaseOrder, "product":uiLabelMap.ProductProduct} >
<@searchFlexAreaJS searchOption=searchOption />

<#-- set estimated delivery date of all items to one value -->
<script type="text/javascript">
  /*<![CDATA[*/

  function set_all_date() {
    var input = document.getElementById('estimatedReadyDateAll');
    if (input) {
      var value = input.value;
      var formEstimatedReadyDate = document.getElementById('updateProductDeliveryDate');
      var inputs = formEstimatedReadyDate.getElementsByTagName('input');
      for (var x = 0; x < inputs.length; x++) {
        if (inputs[x].name.substr(0, 19).match('estimatedReadyDate_') ) inputs[x].value = value;
      }
    }
    return false;
  }
  /*]]>*/
</script>

<#-- Lookup from for a Purchase Order -->
<div class="subSectionBlock">
  <form method="GET" action="<@ofbizUrl>editProductDeliveryDate</@ofbizUrl>" name="editProductDeliveryDate">
    <input type="hidden" id="searchOption" name="searchOption" value="${searchOption}"/>

    <@searchFlexArea searchBy="product" searchOption=searchOption >
      <table class="twoColumnForm">
        <@switchSearchRow title=uiLabelMap.OpentapsFindBy selected="product" choices=searchChoices />
        <tr>
          <@displayTitleCell title=uiLabelMap.ProductProductId />
          <td>
            <@inputLookup name="productId" lookup="LookupProduct" form="editProductDeliveryDate"/>
            <@displayError name="productId"/>
          </td>
        </tr>
        <@inputSubmitRow title=uiLabelMap.CrmFindProduct />
      </table>
    </@searchFlexArea>

    <@searchFlexArea searchBy="order" searchOption=searchOption >
      <table class="twoColumnForm">
        <@switchSearchRow title=uiLabelMap.OpentapsFindBy selected="order" choices=searchChoices />
        <tr>
          <@displayTitleCell title=uiLabelMap.OrderOrderId />
          <td>
            <@inputLookup name="orderId" lookup="LookupPurchaseOrder" form="editProductDeliveryDate"/>
            <@displayError name="orderId"/>
          </td>
        </tr>
        <@inputSubmitRow title=uiLabelMap.OpentapsFindOrder />
      </table>
    </@searchFlexArea>

  </form>
</div>

<#-- Order Items -->
<#if order?exists>
  <#if orderItems?exists && orderItems.size() != 0>
  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.OrderOrderItems >
      <div class="subMenuBar">
        <a href='orderview?orderId=${orderId}' class='subMenuButton'>${uiLabelMap.OrderViewOrder}</a>
      </div>
    </@sectionHeader>

    <form method="post" action="<@ofbizUrl>updateProductDeliveryDate?orderId=${orderId}</@ofbizUrl>" name="updateProductDeliveryDate" id="updateProductDeliveryDate">
      <@inputHidden name="_rowCount" value=orderItems?size />
      <table class="listTable">
        <tr class="listTableHeader">
          <td>${uiLabelMap.CommonDescription}</td>
          <td>${uiLabelMap.OrderQuantity}</td>
          <td>${uiLabelMap.CommonStatus}</td>
          <td align="right">${uiLabelMap.OrderOrderQuoteEstimatedDeliveryDate}</td>
        </tr>

        <#list orderItems as orderItem>
          <@inputHidden name="orderId" value=orderId index=orderItem_index />
          <@inputHidden name="orderItemSeqId" value=orderItem.orderItemSeqId index=orderItem_index />
          <@inputHiddenRowSubmit index=orderItem_index />

          <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
          <tr class="${tableRowClass(orderItem_index)}">
            <td>${orderItem.productId} - ${orderItem.itemDescription?default(orderItem.productId?default(orderItem.comments?default(orderItem.orderItemSeqId)))}</td>
            <td>${orderItem.quantity}</td>
            <td>${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</td>
            <td align="right">
              <#if datesEditable >
                <@inputDate name="estimatedReadyDate" index=orderItem_index default=orderItem.estimatedReadyDate />
              <#else >
                <@displayDate date=orderItem.estimatedReadyDate format="DATE" />
              </#if >
            </td>
          </tr>
        </#list>

        <tr><td colspan="4"><hr class="sepbar"/></td></tr>

        <#if datesEditable >
        <tr>
          <td colspan="4" align="right">
            <a class="buttontext" href="#" onclick="return set_all_date();">${uiLabelMap.PurchSetAllRowsToThisDate}</a> 
            <@inputDate name="estimatedReadyDateAll" />
          </td>
        </tr>
        </#if>

        <tr>
          <td colspan="3"/>
          <@inputSubmitCell title=uiLabelMap.CommonUpdate blockClass="textright" />
        </tr>

      </table>
    </form>

  </div>
  <#elseif orderItems?exists && orderItems.size() == 0 >
    <div class="form"><span class="tableheadtext">${uiLabelMap.PurchNoOrdersFound}</span></div>
  </#if>
<#elseif product?exists>
  <#if orderItems?exists && orderItems.size() != 0>
  <div class="subSectionBlock">
    <@sectionHeader title=uiLabelMap.OpentapsPurchaseOrders />

    <form method="post" action="<@ofbizUrl>updateProductDeliveryDate?productId=${parameters.productId}</@ofbizUrl>" name="updateProductDeliveryDate" id="updateProductDeliveryDate">
      <@inputHidden name="_rowCount" value=orderItems?size />
      <table class="listTable">
        <tr class="listTableHeader">
          <td>${uiLabelMap.OrderOrderId}</td>
          <td>${uiLabelMap.OrderQuantity}</td>
          <td>${uiLabelMap.CommonStatus}</td>
          <td align="right">${uiLabelMap.OrderOrderQuoteEstimatedDeliveryDate}</td>
        </tr>

        <#list orderItems as orderItem>
          <@inputHidden name="orderId" value=orderItem.orderId index=orderItem_index />
          <@inputHidden name="orderItemSeqId" value=orderItem.orderItemSeqId index=orderItem_index />
          <@inputHiddenRowSubmit index=orderItem_index />

          <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
          <tr class="${tableRowClass(orderItem_index)}">
            <@displayLinkCell href="orderview?orderId=${orderItem.orderId}" text=orderItem.orderId class="linktext" />
            <td>${orderItem.quantity}</td>
            <td>${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</td>
            <td align="right">
                <@inputDate name="estimatedReadyDate" index=orderItem_index default=orderItem.estimatedReadyDate />
            </td>
          </tr>
        </#list>

        <tr><td colspan="4"><hr class="sepbar"/></td></tr>

        <tr>
          <td colspan="4" align="right">
            <a class="buttontext" href="#" onclick="return set_all_date();">${uiLabelMap.PurchSetAllRowsToThisDate}</a>
            <@inputDate name="estimatedReadyDateAll" />
          </td>
        </tr>

        <tr>
          <td colspan="3"/>
          <@inputSubmitCell title=uiLabelMap.CommonUpdate blockClass="textright" />
        </tr>

      </table>
    </form>

  </div>
  <#elseif orderItems?exists && orderItems.size() == 0 >
    <div class="form"><span class="tableheadtext">${uiLabelMap.PurchNoOrdersFound}</span></div>
  </#if>
</#if>