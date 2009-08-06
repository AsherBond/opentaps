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

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmShortcuts}</div></div>
    <div class="screenlet-body">
      <ul class="shortcuts">
        <li><a href="<@ofbizUrl>myContacts</@ofbizUrl>">${uiLabelMap.CrmMyContacts}</a></li>
        <#if (security.hasEntityPermission("CRMSFA_CONTACT", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createContactForm</@ofbizUrl>">${uiLabelMap.CrmCreateContact}</a></li>
        </#if>
        <li><a href="<@ofbizUrl>findContacts</@ofbizUrl>">${uiLabelMap.CrmFindContacts}</a></li>
        <li><a href="<@ofbizUrl>mergeContactsForm</@ofbizUrl>">${uiLabelMap.CrmMergeContacts}</a></li>
      </ul>
    </div>
</div>
