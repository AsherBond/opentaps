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

<#if order?has_content>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderInfo.ftl"/>
      <@include location="component://opentaps-common/webapp/common/order/orderTerms.ftl"/>
    </td>
    <td width="10" nowrap="nowrap">&nbsp;</td>
    <td width="50%" valign="top">
      <@include location="component://opentaps-common/webapp/common/order/orderContactInfo.ftl"/>
    </td>
  </tr>
</table>

<@include location="component://opentaps-common/webapp/common/order/editOrderItems.ftl"/>
<@include location="component://opentaps-common/webapp/common/order/appendOrderItem.ftl"/>

<#else>
<p class="tableheadtext">${uiLabelMap.OrderNoOrderFound}</p>
</#if>
