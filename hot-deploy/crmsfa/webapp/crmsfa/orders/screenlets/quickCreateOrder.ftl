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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if (security.hasEntityPermission("CRMSFA_ORDER", "_CREATE", session))>
<form name="quickCreateOrderForm" method="post" action="<@ofbizUrl>quickCreateOrder</@ofbizUrl>">
<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.OrderNewOrder}</div></div>
    <div class="screenlet-body">

      <span class="requiredFieldNormal">${uiLabelMap.ProductCustomer}</span><br/>
      <@inputAutoCompleteClient name="partyId" id="quickCreateOrderFormPartyId" size=10 />
      <br/>

      <span class="tabletext">${uiLabelMap.OrderOrderName}</span><br/>
      <@inputText name="orderName" size=15 maxlength=200/>
      <br/>

      <span class="tabletext">${uiLabelMap.OrderShipBeforeDate}</span><br/>
      <@inputDate name="shipBeforeDate"/>
      <br/>

      <span class="tabletext">${uiLabelMap.ProductProductId}</span><br/>
      <@inputAutoCompleteProduct name="productId" id="quickCreateOrderFormProductId" size=10 />
      <br/>

      <span class="tabletext">${uiLabelMap.CommonQuantity}</span><br/>
      <@inputText name="quantity" size=10 maxlength=10 default="1"/>
      <br/>

      <@inputSubmit title=uiLabelMap.CommonCreate/>
    </div>
</div>
</form>
</#if>
