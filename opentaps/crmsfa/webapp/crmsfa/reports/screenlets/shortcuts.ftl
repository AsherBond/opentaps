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

<@frameSection title=uiLabelMap.CrmShortcuts>
  <ul class="shortcuts">
    <#if (security.hasEntityPermission("CRMSFA_RPT", "_VIEW", session))>
      <li><a href="<@ofbizUrl>viewDashboard</@ofbizUrl>">${uiLabelMap.CrmDashboard}</a></li>
      <li><a href="<@ofbizUrl>manageReports</@ofbizUrl>">${uiLabelMap.OpentapsReports}</a></li>
    </#if>
  </ul>
</@frameSection>
