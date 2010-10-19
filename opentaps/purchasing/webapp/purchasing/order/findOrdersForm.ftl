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

<div class="subSectionBlock">
<form method="get" target="" name="findOrdersForm">
<table class="twoColumnForm">
  <@inputTextRow name="orderId" title=uiLabelMap.OrderOrderId />
  <@inputTextRow name="orderName" title=uiLabelMap.OrderOrderName />
  <@inputLookupRow name="productPattern" title=uiLabelMap.ProductProduct lookup="LookupProduct" form="findOrdersForm" size=26 />
  <@inputLookupRow name="supplierPartyId" title=uiLabelMap.ProductSupplier lookup="LookupPartyGroup" form="findOrdersForm" />
  <@inputSelectRow name="statusId" list=statusItems title=uiLabelMap.CommonStatus displayField="description" required=false />
  <@inputDateTimeRow name="fromDate" title=uiLabelMap.CommonFromDate form="findOrdersForm" />
  <@inputDateTimeRow name="thruDate" title=uiLabelMap.CommonThruDate form="findOrdersForm" />
  <@inputTextRow name="createdBy" title=uiLabelMap.CommonCreatedBy />
  <@inputHidden name="performFind" value="Y" />
  <@inputSubmitRow title=uiLabelMap.CommonFind />
</table>
</form>
</div>
