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

<#if (security.hasEntityPermission("CRMSFA_ACCOUNT", "_CREATE", session))>
  <@gwtWidget id="quickNewAccount" class="gwt-screenlet"/>
</#if>
<#if (security.hasEntityPermission("CRMSFA_CONTACT", "_CREATE", session))>
  <@gwtWidget id="quickNewContact" class="gwt-screenlet"/>
</#if>
<#if (security.hasEntityPermission("CRMSFA_CASE", "_CREATE", session))>
  <@gwtWidget id="quickNewCase" class="gwt-screenlet"/>
</#if>
<#if (security.hasEntityPermission("CRMSFA_LEAD", "_CREATE", session))>
  <@gwtWidget id="quickNewLead" class="gwt-screenlet"/>
</#if>
<#if (security.hasEntityPermission("CRMSFA_OPP", "_CREATE", session))>
  <@gwtWidget id="quickNewOpportunity" class="gwt-screenlet"/>
</#if>
