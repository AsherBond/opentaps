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
        <#if viewPreferences.get("MY_OR_TEAM_ACCOUNTS")?default("TEAM_VALUES") == "MY_VALUES">
        <li><a href="<@ofbizUrl>myAccounts</@ofbizUrl>">${uiLabelMap.CrmMyAccounts}</a></li>
        <#else>
        <li><a href="<@ofbizUrl>myAccounts</@ofbizUrl>">${uiLabelMap.CrmTeamAccounts}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_ACCOUNT", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createAccountForm</@ofbizUrl>">${uiLabelMap.CrmCreateAccount}</a></li>
        </#if>
        <li><a href="<@ofbizUrl>findAccounts</@ofbizUrl>">${uiLabelMap.CrmFindAccounts}</a></li>
        <li><a href="<@ofbizUrl>mergeAccountsForm</@ofbizUrl>">${uiLabelMap.CrmMergeAccounts}</a></li>
      </ul>
    </div>
</div>
