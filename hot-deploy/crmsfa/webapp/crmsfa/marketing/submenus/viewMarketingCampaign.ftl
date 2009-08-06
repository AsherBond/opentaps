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

<#if hasUpdatePermission?exists>
<#if marketingCampaign.statusId == "MKTG_CAMP_INPROGRESS">
<#assign writeEmail = "<a class='subMenuButton' href='writeEmail?emailType=MKTG_CAMPAIGN&marketingCampaignId=" + marketingCampaign.marketingCampaignId + "&donePage=viewMarketingCampaign'>" + uiLabelMap.CrmWriteEmail + "</a>">
</#if>
<#assign updateLink = "<a class='subMenuButton' href='updateMarketingCampaignForm?marketingCampaignId=" + marketingCampaign.marketingCampaignId + "'>" + uiLabelMap.CommonEdit + "</a>">
</#if>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.CrmMarketingCampaign}</div>
  <div class="subMenuBar">${writeEmail?if_exists}${updateLink?if_exists}</div>
</div>
