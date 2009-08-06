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
      <#if viewPreferences.get("MY_OR_TEAM_OPPS")?default("TEAM_VALUES") == "MY_VALUES">
        <li><a href="<@ofbizUrl>myOpportunities</@ofbizUrl>">${uiLabelMap.CrmMyOpportunities}</a></li>
      <#else>
        <li><a href="<@ofbizUrl>myOpportunities</@ofbizUrl>">${uiLabelMap.CrmTeamOpportunities}</a></li>
      </#if>
        <li><a href="<@ofbizUrl>createOpportunityForm</@ofbizUrl>">${uiLabelMap.CrmCreateOpportunity}</a></li>
        <li><a href="<@ofbizUrl>findOpportunities</@ofbizUrl>">${uiLabelMap.CrmFindOpportunities}</a></li>
        <#if (security.hasEntityPermission("CRMSFA_QUOTE", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createQuote</@ofbizUrl>">${uiLabelMap.PartyCreateNewQuote}</a></li>
        </#if>
      </ul>
    </div>
</div>
