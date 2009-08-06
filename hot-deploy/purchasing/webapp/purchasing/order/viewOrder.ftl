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

<#-- 
Instead of using a screen to do the layout, we use an ftl.  
This is because doing multicolumn layout in CSS as OFBiz does is really
difficult (CSS is weak in this respect).

This may also be the platform for refactoring order header. 

Notice how we replaced the screens.render with #include directives.  These subscreens do not have
any special data that is not set up in orderview.bsh, so the screen render call is redundant.
If special data is needed, either add a new bsh to run after orderview.bsh or include in orderview.bsh.
The whole idea is to keep things simple and not use the screen widget where unnecessary.
-->

<#if orderHeader?has_content && orderHeader.orderTypeId == "PURCHASE_ORDER">

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

<#include "orderview/orderitems.ftl"/>
<#include "orderview/ordernotes.ftl"/>
<#include "orderview/transitions.ftl"/>

<#else>
<p class="tableheadtext">${uiLabelMap.OrderNoOrderFound}</p>
</#if>
