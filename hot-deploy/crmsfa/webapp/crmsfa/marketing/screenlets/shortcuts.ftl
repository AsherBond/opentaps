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

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmShortcuts}</div></div>
    <div class="screenlet-body">
      <ul class="shortcuts">
        <li><a href="<@ofbizUrl>createMarketingCampaignForm</@ofbizUrl>">${uiLabelMap.CrmCreateMarketingCampaign}</a></li>
        <li><a href="<@ofbizUrl>findMarketingCampaigns</@ofbizUrl>">${uiLabelMap.CrmFindMarketingCampaigns}</a></li>
        <li><a href="<@ofbizUrl>createContactListForm</@ofbizUrl>">${uiLabelMap.CrmCreateContactList}</a></li>
        <li><a href="<@ofbizUrl>findContactLists</@ofbizUrl>">${uiLabelMap.CrmFindContactLists}</a></li>
        <li><a href="<@ofbizUrl>listSurveys</@ofbizUrl>">${uiLabelMap.CrmMarketingSurveyResults}</a></li>
        <li><a href="<@ofbizUrl>marketingReports</@ofbizUrl>">${uiLabelMap.CommonReports}</a></li>
        <#if (security.hasEntityPermission("CRMSFA_CAMP", "_CREATE", session))>
        <li><a href="<@ofbizUrl>ManageMergeForms</@ofbizUrl>">${uiLabelMap.CrmFormLetterTemplates}</a></li>
        </#if>
      </ul>
    </div>
</div>
