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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if (security.hasEntityPermission("CRMSFA_ORDER", "_CREATE", session))>

<@frameSection title=uiLabelMap.OrderNewOrder>
  <form name="quickCreateOrderForm" method="post" action="<@ofbizUrl>quickCreateOrder</@ofbizUrl>">
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
  </form>
</@frameSection>

</#if>
