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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if hasUpdatePermission?exists>
<#assign updateLink = "<a class='subMenuButton' href='updateSupplierForm?partyId=" + partySummary.partyId + "'>" + uiLabelMap.CommonEdit + "</a>">
</#if>

<div class="subSectionBlock">

  <@sectionHeader title=uiLabelMap.ProductSupplier>
    <div class="subMenuBar">${updateLink?if_exists}</div>
  </@sectionHeader>

  <table class="twoColumnForm">
    <@displayRow title=uiLabelMap.CommonName text="${partySummary.groupName} (${partySummary.partyId})" />
    <@displayRow title=uiLabelMap.OpentapsTaxAuthPartyId text=partySummary.federalTaxId />
    <@displayIndicatorRow title=uiLabelMap.OpentapsRequires1099 value=partySummary.requires1099?default("") />
    <@displayIndicatorRow title=uiLabelMap.OpentapsIsIncorporated value=partySummary.isIncorporated?default("") />
  </table>

</div>

