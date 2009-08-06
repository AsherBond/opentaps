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

<#if order?has_content>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderInfo.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderTerms.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderPaymentInfo.ftl"/>
    </td>
    <td width="10" nowrap="nowrap">&nbsp;</td>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderContactInfo.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderShippingInfo.ftl"/>
    </td>
  </tr>
</table>

<@include location="component://opentaps-common/webapp/common/order/editOrderItems.ftl"/>
<@include location="component://opentaps-common/webapp/common/order/appendOrderItem.ftl"/>

<#else/>
  <p class="tableheadtext">${uiLabelMap.CrmOrderNotFound}</p>
</#if>
