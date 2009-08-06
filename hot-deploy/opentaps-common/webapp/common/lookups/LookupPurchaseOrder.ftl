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

<@sectionHeader title=uiLabelMap.OpentapsFindPurch />

<div class="subSectionBlock">
<form method="post" target="" name="findOrdersForm">
<table class="twoColumnForm">
  <@inputTextRow name="orderId" title=uiLabelMap.OrderOrderId default=parameters.orderId?if_exists />
  <@inputTextRow name="orderName" title=uiLabelMap.OrderOrderName default=parameters.orderName?if_exists />
  <@inputTextRow name="supplierPartyId" title=uiLabelMap.ProductSupplier default=parameters.supplierPartyId?if_exists />
  <@inputSelectRow name="statusId" list=statusItems title=uiLabelMap.CommonStatus displayField="description" default=parameters.statusId?if_exists required=false />
  <@inputHidden name="performFind" value="Y" />
  <@inputSubmitRow title=uiLabelMap.CommonFind />
</table>
</form>
</div>
