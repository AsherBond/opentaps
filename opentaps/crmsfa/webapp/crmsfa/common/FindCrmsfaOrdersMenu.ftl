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

<#if viewPreferences.get("MY_OR_TEAM_ORDERS")?default("MY_VALUES") == "MY_VALUES">
  <#assign title = uiLabelMap.CrmMyOrders />
  <#assign prefValue = "ALL_OPEN_ORDERS" />
  <#assign prefButtonLabel = uiLabelMap.CrmOpenOrders />
<#else> 
  <#assign title = uiLabelMap.CrmOpenOrders />
  <#assign prefValue = "MY_VALUES"/>
  <#assign prefButtonLabel = uiLabelMap.CrmMyOrders />
</#if>

<@form name="MyOrdersPrefChangeForm" url="setViewPreference" viewPrefValue="${prefValue}" viewPrefTypeId="MY_OR_TEAM_ORDERS" donePage="myOrders"/>

<#assign extraOptions>
  <@submitFormLink form="MyOrdersPrefChangeForm" text="${prefButtonLabel}" />
</#assign>

<@frameSectionTitleBar title=title?if_exists titleClass="sectionHeaderTitle" titleId="sectionHeaderTitle_${sectionName?if_exists}" extra=extraOptions/>
