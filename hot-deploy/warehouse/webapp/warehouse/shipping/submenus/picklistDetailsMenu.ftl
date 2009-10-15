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
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if picklistInfo?has_content>
<#if isPicklistPicked?exists>
  <@form name="closePicklistAction" url="closePicklist" picklistId=picklistInfo.picklistId />
</#if>
<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.WarehousePicklistDetails}</div>
  <div class="subMenuBar">
    <a href="<@ofbizUrl>PicklistReport.pdf?picklistId=${picklistInfo.picklistId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.OpentapsContentType_ApplicationPDF}</a>
    <#if isPicklistPicked?exists>
      <@submitFormLink form="closePicklistAction" class="subMenuButton" text=uiLabelMap.WarehouseClosePicklists />
    </#if>
  </div>
</div>
</#if>
