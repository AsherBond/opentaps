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
<form method="post" target="" name="findOrdersForm">
<table>
  <@inputTextRow name="orderId" title=uiLabelMap.OrderOrderId default=parameters.orderId?if_exists />
  <@inputTextRow name="orderName" title=uiLabelMap.OrderOrderName default=parameters.orderName?if_exists />
  <@inputAutoCompleteSupplierRow name="supplierPartyId" title=uiLabelMap.ProductSupplier default=parameters.supplierPartyId?if_exists />
  <@inputSelectRow name="statusId" list=statusItems title=uiLabelMap.CommonStatus displayField="description" default=parameters.statusId?if_exists required=false />
  <@inputHidden name="performFind" value="Y" />
  <@inputSubmitRow title=uiLabelMap.CommonFind />
</table>
</form>
