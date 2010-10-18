
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

<#-- TODO refactor to be common to all checkout steps? this is for review order now. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#assign extraOptions><@inputConfirm href="clearCart" title=uiLabelMap.OpentapsClearItems class="subMenuButtonDangerous"/><a class="subMenuButton" href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${uiLabelMap.OpentapsOrderReturnToOrder}</a><a class="subMenuButton" href="<@ofbizUrl>crmsfaQuickCheckout</@ofbizUrl>">${uiLabelMap.CommonOptions}</a><a class="subMenuButton" href="#" onclick="redirectUrlAndDisableLink('<@ofbizUrl>processorder</@ofbizUrl>',this,'${uiLabelMap.OpentapsOrderSubmittingLabel}')">${uiLabelMap.OrderCreateOrder}</a></#assign>

<@frameSectionHeader title=uiLabelMap.OrderReviewOrder extra=extraOptions />
