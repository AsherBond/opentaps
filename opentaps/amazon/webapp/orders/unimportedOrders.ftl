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

<@paginate name="amazonOrders" list=amazonOrders>
  <#noparse>
    <@navigationHeader/>
    <table class="listTable" style="max-width:100%">
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup><col/></colgroup>
      <colgroup style="max-width:150px"><col/></colgroup>
      <colgroup><col/></colgroup>
      <tr class="listTableHeader">
        <@displayCell text=uiLabelMap.AmazonOrderAmazonOrderId blockStyle="max-width:125px"/>
        <@displayCell text=uiLabelMap.AmazonOrderOrderDate blockStyle="max-width:75px"/>
        <td class="tabletext" style="max-width:150px"/>
          <@display text=uiLabelMap.AmazonOrderBuyerName/><br/>
          <@display text=uiLabelMap.AmazonOrderBuyerEmailAddress/><br/>
          <@display text=uiLabelMap.AmazonOrderBuyerPhoneNumber/>
        </td>
        <@displayCell text=uiLabelMap.AmazonOrderAddress blockStyle="max-width:75px"/>
        <@displayCell text=uiLabelMap.AmazonOrderFulfillmentServiceLevel blockStyle="max-width:75px"/>
        <@displayCell text=uiLabelMap.AmazonOrderStatus blockStyle="max-width:75px"/>
        <@displayCell text=uiLabelMap.AmazonOrderLastImportAttempt blockStyle="max-width:75px"/>
        <@displayCell text=uiLabelMap.AmazonOrderImportFailures blockStyle="max-width:50px"/>
        <@displayCell text=uiLabelMap.AmazonOrderImportErrorMessage blockStyle="max-width:150px"/>
        <td>&nbsp;</td>
      </tr>
      <#list pageRows as amazonOrder>
        <tr class="${tableRowClass(amazonOrder_index)}">
          <@displayLinkCell href="javascript:opentaps.expandCollapse('${amazonOrder.amazonOrderId}')" text=amazonOrder.amazonOrderId/>
          <td class="tabletext">
            <@display text=amazonOrder.orderDate?date/><br/>
            <@display text=amazonOrder.orderDate?time/>
          </td>
          <td class="tabletext">
            <@display text=amazonOrder.buyerName/><br/>
            <@display text=amazonOrder.buyerEmailAddress/><br/>
            <@display text=amazonOrder.buyerPhoneNumber/>
          </td>
          <td class="tabletext">
            <@display text=amazonOrder.addressName/><br/>
            <@display text=amazonOrder.addressFieldOne/><br/>
            <#if amazonOrder.addressFieldTwo?has_content><@display text=amazonOrder.addressFieldTwo/><br/></#if>
            <#if amazonOrder.addressFieldThree?has_content><@display text=amazonOrder.addressFieldThree/><br/></#if>
            <@display text=amazonOrder.addressCity/><br/>
            <@display text=amazonOrder.addressStateOrRegion/>,&nbsp;
            <@display text=amazonOrder.addressCountryCode/><br/>
            <@display text=amazonOrder.addressPostalCode/><br/>
            <@display text=amazonOrder.addressPhoneNumber/>
          </td>
          <@displayCell text=amazonOrder.fulfillmentServiceLevel/>
          <@displayCell text=amazonOrder.statusDescription/>
          <td class="tabletext">
            <#if amazonOrder.importTimestamp?exists>
                <@display text=amazonOrder.importTimestamp?date/><br/>
                <@display text=amazonOrder.importTimestamp?time/>
            </#if>
          </td>
          <@displayCell text=amazonOrder.importFailures/>
          <@displayCell text=amazonOrder.importErrorMessage/>
          <td align="right" style="padding-right:5px">
            <#if "AMZN_ORDR_NOT_ACKED" != amazonOrder.statusId>
              <div><@displayLink text=uiLabelMap.CommonCancel href="CancelUnimportedOrder?amazonOrderId=${amazonOrder.amazonOrderId}" class="linktext"/></div>
            </#if>
          </td>
        </tr>
        <tr class="${tableRowClass(amazonOrder_index)}">
          <td colspan="10">
            <@flexArea targetId="${amazonOrder.amazonOrderId}" style="border:none" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
              <table class="listTable">
                <tr class="listTableHeader" style="background-color:white">
                  <@displayCell text=uiLabelMap.AmazonOrderItemCode/>
                  <@displayCell text=uiLabelMap.AmazonOrderSKU/>
                  <@displayCell text=uiLabelMap.AmazonOrderTitle/>
                  <@displayCell text=uiLabelMap.AmazonOrderQuantity blockStyle="text-align:right"/>
                </tr>
                <#assign orderItems = amazonOrder.orderItems/>
                <#list orderItems as orderItem>
                  <tr class="${tableRowClass(orderItem_index)}">
                    <@displayCell text=orderItem.amazonOrderItemCode/>
                    <@displayCell text=orderItem.sku/>
                    <@displayCell text=orderItem.title/>
                    <@displayCell text=orderItem.quantity blockStyle="text-align:right"/>
                  </tr>
                </#list>
              </table>
            </@flexArea>
          </td>
        </tr>
      </#list>
    </table>
  </#noparse>
</@paginate>
