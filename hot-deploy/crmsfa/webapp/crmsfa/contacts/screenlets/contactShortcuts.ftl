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

<#if validView?default(false) && !contactDeactivated?exists>

<#if hasViewOrderPermission?exists>
  <#if continueOrder>
    <#assign orderLink = "<a href='resumeOrder?partyId="+parameters.partyId+"'>"+uiLabelMap.OpentapsResumeOrder+"</a>" />
  <#else>
    <#assign orderLink = "<a href='newOrder?partyId="+parameters.partyId+"'>"+uiLabelMap.OpentapsCreateOrder+"</a>" />
  </#if>
</#if>

<div class="screenlet">
    <div class="screenlet-header"><div class="boxhead">${uiLabelMap.CrmContactShortcuts}</div></div>
    <div class="screenlet-body">
      <ul class="shortcuts">
        <#if orderLink?exists>
          <li>${orderLink}</li>
        </#if>
      </ul>
    </div>
</div>

</#if>
