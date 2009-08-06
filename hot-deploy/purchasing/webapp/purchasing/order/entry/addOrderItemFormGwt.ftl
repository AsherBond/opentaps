<#--
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

<#if shoppingCart.size() == 0>
  <#assign hideButtons = "style='visibility:hidden'" />
</#if>

<#assign finalizeLink = "<a id='finalizeOrderButton' class='subMenuButton' ${hideButtons?if_exists} href='finalizeOrder?finalizeMode=init'>${uiLabelMap.OpentapsFinalizeOrder}</a>" />

<script type="text/javascript">
/*<![CDATA[*/
function notifyOrderItemsCount(n) {
  var button = document.getElementById('finalizeOrderButton');
  if (n > 0) {
    button.style.visibility = 'visible';
  } else {
    button.style.visibility = 'hidden';
  }
}
/*]]>*/
</script>

<div id="sectionHeaderTitle_order" class="sectionHeader sectionHeaderTitle">
  <span style="float:left;">${uiLabelMap.OrderOrders}</span>
  <div class="subMenuBar" style="float:right; margin:0.4em">${finalizeLink?if_exists}</div>
</div>

<div style="float:right;padding:2px;"><span class="toggleButtonDisabled">${uiLabelMap.OpentapsGridView}</span><a class="toggleButton" href="createOrderMainScreen?useGwt=N">${uiLabelMap.OpentapsFullView}</a></div>
<div class="cleaner">&nbsp;</div>

<@gwtWidget id="orderItemsEntryGrid" organizationPartyId=organizationPartyId />
