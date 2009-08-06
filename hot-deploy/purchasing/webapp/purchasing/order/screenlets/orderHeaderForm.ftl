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

<#if shoppingCart?has_content>

  <@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
  
  <form name="updateOrderHeaderInfo" action="<@ofbizUrl>updateOrderHeaderInfo</@ofbizUrl>" method="POST">
  
    <div class="screenlet">
        <div class="screenlet-header">
          <div class="boxhead">${uiLabelMap.OpentapsOrderSettings}</div>
        </div>
        <div class="screenlet-body">

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


            <div class="orderEntryHeaderFormRow">
              <span class="tableheadtext orderEntryHeaderFormLabel">
                ${uiLabelMap.CommonTotal}:
              </span>
              <span class="tableheadtext" style="padding-right:24px">
                <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=shoppingCart.getCurrency()/>
              </span>
            </div>
  
            <div class="orderEntryHeaderSubmitRow">
              <@inputConfirm title=uiLabelMap.OpentapsCancelOrder form="cancelOrderForm"/>
            </div>
            
          </div>
        </div>
    </div>
  </form>

<form name="cancelOrderForm" method="POST" action="destroyCart">
</form>

</#if>
