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

<#if shoppingCart?has_content>

  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
  
  <form name="updateOrderHeaderInfo" action="<@ofbizUrl>updateOrderHeaderInfo</@ofbizUrl>" method="POST">
  
    <@frameSection title=uiLabelMap.OpentapsOrderSettings>
      <div id="orderEntryHeaderForm">

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.PartySupplier}:
          </span>
          <span class="tableheadtext">
            ${supplierName}
          </span>
        </div>

        <div class="orderEntryHeaderFormRow">
          <span class="tableheadtext orderEntryHeaderFormLabel">
            ${uiLabelMap.CommonCurrency}:
          </span>
          <span class="tableheadtext">
            ${shoppingCart.getCurrency()}
          </span>
        </div>

        <#if ! useGwt!false>
          <div class="orderEntryHeaderFormRow">
            <span class="tableheadtext orderEntryHeaderFormLabel">
              ${uiLabelMap.CommonTotal}:
            </span>
            <span class="tableheadtext" style="padding-right:24px">
              <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=shoppingCart.getCurrency()/>
            </span>
          </div>
        </#if>
  
        <div class="orderEntryHeaderSubmitRow">
          <@inputConfirm title=uiLabelMap.OpentapsCancelOrder form="cancelOrderForm"/>
        </div>
      </div>
    </@frameSection>
  </form>

<form name="cancelOrderForm" method="POST" action="destroyCart">
</form>

</#if>
