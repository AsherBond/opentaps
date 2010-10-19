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

<#if shoppingCart.size() == 0>
  <#assign hideButtons = "style='display:none'" />
</#if>
<#if shoppingCart.size() == 0 || !hasParty>
  <#assign hideFinalizeOrderButton = "style='display:none'" />
</#if>
<#if hasParty>
  <#assign quoteLink = "<a id='quoteButton' class='subMenuButton' ${hideButtons?if_exists} href='createQuoteFromCart'>${uiLabelMap.CrmSaveAsQuote}</a>" />
</#if>
<#assign newCustomerLink = "<a id='newCustomerButton' class='subMenuButton' ${hideButtons?if_exists} href='createOrderPartyForm'>${uiLabelMap.CrmCreateNewCustomer}</a>" />
<#assign finalizeLink = "<a id='finalizeOrderButton' class='subMenuButton' ${hideFinalizeOrderButton?if_exists} href='finalizeOrder?finalizeMode=init'>${uiLabelMap.OpentapsFinalizeOrder}</a>" />

<script type="text/javascript">
/*<![CDATA[*/
function notifyOrderItemsCount(n) {
  var button = document.getElementById('finalizeOrderButton');
  if (n > 0 && ${hasParty?string}) {
    button.style.display = '';
  } else {
    button.style.display = 'none';
  }
  button = document.getElementById('quoteButton');
  if (button) {
    if (n > 0) {
      button.style.display = '';
    } else {
      button.style.display = 'none';
    }
  }
  button = document.getElementById('newCustomerButton');
  if (button) {
    if (n > 0) {
      button.style.display = '';
    } else {
      button.style.display = 'none';
    }
  }
}
/*]]>*/
</script>

<@frameSectionTitleBar title=uiLabelMap.OrderOrders titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_orders" extra="${newCustomerLink?if_exists}${quoteLink?if_exists}${finalizeLink?if_exists}" />

<div style="float:right;padding:2px;"><span class="toggleButtonDisabled">${uiLabelMap.OpentapsGridView}</span><a class="toggleButton" href="createOrderMainScreen?useGwt=N">${uiLabelMap.OpentapsFullView}</a></div>
<div class="cleaner">&nbsp;</div>

<@gwtWidget id="orderItemsEntryGrid" organizationPartyId=organizationPartyId />
