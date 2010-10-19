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
<#-- Copyright (c) Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionBlock">

<@sectionHeader title=uiLabelMap.CrmOpenOrders>
    <div class="subMenuBar">
        <@displayLink href="findOrders?partyIdSearch=${parameters.partyId?if_exists}&performFind=Y" text="${uiLabelMap.OrderOrders}" class="subMenuButton"/>
        <@displayLink href="findQuotes?partyId=${parameters.partyId?if_exists}&performFind=Y" text="${uiLabelMap.OrderOrderQuotes}" class="subMenuButton"/>
    </div>
</@sectionHeader>

<#if hasViewOrderPermission?exists>
    <@gwtWidget id="accountOpenOrdersSubsection" partyId="${partySummary.partyId}"/>
</#if>

</div>
