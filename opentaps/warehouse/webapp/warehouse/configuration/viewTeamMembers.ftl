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

<#-- TODO: replace with macros -->

<#if hasConfigPermission>

<form action="<@ofbizUrl>addFacilityTeamMember</@ofbizUrl>" method="post" name="addFacilityTeamMember">
  <input type="hidden" name="facilityId" value="${session.getAttribute("facilityId")}"/>

  <table class="twoColumnForm">
    <tr>
      <td class="titleCell">
        <span class="tableheadtext">${uiLabelMap.OpentapsNewTeamMember}</span>
      </td>
      <td>
        <input type="text" size="20" name="partyId" class="inputBox">
        <a href="javascript:call_fieldlookup2(document.addFacilityTeamMember.partyId,'LookupPerson');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"></a>
      </td>
    </tr>
    <tr>
      <td class="titleCell">
        <span class="tableheadtext">${uiLabelMap.PartyRole}</span>
      </td>
      <td>
        <select name="securityGroupId" class="inputBox">
          <#list roles as role>
          <option value="${role.securityGroupId}">${role.roleDescription}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td class="titleCell"></td>
      <td class="tabletext">
        <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
      </td>
    </tr>

  </table>
</form>

</#if>
