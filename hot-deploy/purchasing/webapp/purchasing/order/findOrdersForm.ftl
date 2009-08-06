<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
