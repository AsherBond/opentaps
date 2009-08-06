
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

<#-- TODO refactor to be common to all checkout steps? this is for review order now. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.OrderReviewOrder}</div>
  <div class="subMenuBar"><@inputConfirm href="clearCart" title=uiLabelMap.OpentapsClearItems class="subMenuButtonDangerous"/><a class="subMenuButton" href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${uiLabelMap.OpentapsOrderReturnToOrder}</a><a class="subMenuButton" href="<@ofbizUrl>crmsfaQuickCheckout</@ofbizUrl>">${uiLabelMap.CommonOptions}</a><a class="subMenuButton" href="#" onclick="redirectUrlAndDisableLink('<@ofbizUrl>processorder</@ofbizUrl>',this,'${uiLabelMap.OpentapsOrderSubmittingLabel}')">${uiLabelMap.OrderCreateOrder}</a></div>
</div>

