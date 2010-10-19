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

<#if hasUpdatePermission?exists>
<#if marketingCampaign.statusId == "MKTG_CAMP_INPROGRESS">
<#assign writeEmail = "<a class='subMenuButton' href='writeEmail?emailType=MKTG_CAMPAIGN&marketingCampaignId=" + marketingCampaign.marketingCampaignId + "&donePage=viewMarketingCampaign'>" + uiLabelMap.CrmWriteEmail + "</a>">
</#if>
<#assign updateLink = "<a class='subMenuButton' href='updateMarketingCampaignForm?marketingCampaignId=" + marketingCampaign.marketingCampaignId + "'>" + uiLabelMap.CommonEdit + "</a>">
</#if>

<@frameSectionHeader title=uiLabelMap.CrmMarketingCampaign extra="${writeEmail?if_exists}${updateLink?if_exists}" />
