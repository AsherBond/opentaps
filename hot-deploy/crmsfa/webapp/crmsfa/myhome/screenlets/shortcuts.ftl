<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<#if session.getAttribute("shoppingCart")?exists>
    <#assign orderLabel = uiLabelMap.OpentapsResumeOrder>
<#else>
    <#assign orderLabel = uiLabelMap.OpentapsCreateOrder>
</#if>

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmShortcuts}</div></div>
    <div class="screenlet-body">
      <ul class="shortcuts">
        <#if (security.hasEntityPermission("CRMSFA_DASH", "_VIEW", session))>
        <li><a href="<@ofbizUrl>viewDashboard</@ofbizUrl>">${uiLabelMap.CrmDashboard}</a></li>
        </#if>
        <li><a href="<@ofbizUrl>myHomeMain</@ofbizUrl>">${uiLabelMap.CrmMyCalendar}</a></li>
        <#if (security.hasEntityPermission("CRMSFA_LEAD", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createLeadForm</@ofbizUrl>">${uiLabelMap.CrmCreateLead}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_ACCOUNT", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createAccountForm</@ofbizUrl>">${uiLabelMap.CrmCreateAccount}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_CONTACT", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createContactForm</@ofbizUrl>">${uiLabelMap.CrmCreateContact}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_OPP", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createOpportunityForm</@ofbizUrl>">${uiLabelMap.CrmCreateOpportunity}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_QUOTE", "_CREATE", session))>
        <li><a href="<@ofbizUrl>EditQuote</@ofbizUrl>">${uiLabelMap.PartyCreateNewQuote}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_CASE", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createCaseForm</@ofbizUrl>">${uiLabelMap.CrmCreateCase}</a></li>
        </#if>
        <#if (security.hasEntityPermission("CRMSFA_ORDER", "_CREATE", session))>
        <li><a href="<@ofbizUrl>createOrderMainScreen</@ofbizUrl>">${orderLabel}</a></li>
        </#if>
        <li><a href="<@ofbizUrl>writeEmail</@ofbizUrl>">${uiLabelMap.CrmWriteEmail}</a></li>
        <li><a href="<@ofbizUrl>logTaskForm?workEffortPurposeTypeId=WEPT_TASK_PHONE_CALL</@ofbizUrl>">${uiLabelMap.CrmLogCall}</a></li>
        <li><a href="<@ofbizUrl>createTaskForm?workEffortTypeId=TASK</@ofbizUrl>">${uiLabelMap.CrmCreateNewTask}</a></li>
        <li><a href="<@ofbizUrl>createEventForm?workEffortTypeId=EVENT</@ofbizUrl>">${uiLabelMap.CrmCreateNewEvent}</a></li>
      </ul>
    </div>
</div>
