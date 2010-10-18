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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl" />

<#if team.members?has_content>

  <table class="crmsfaListTable">
    <tr class="crmsfaListTableHeader">
      <td><span class="tableheadtext">${uiLabelMap.CommonName}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.PartyRole}</span></td>
      <#if hasTeamUpdatePermission!false><td/></#if>
      <#if hasTeamRemovePermission!false><td/></#if>
    </tr>
    <#list team.members as member>
      <tr class="${tableRowClass(member_index)}">
        <@displayCell text="${member.name} (${member.partyId})" />
        <#if hasTeamUpdatePermission!false>
          <@form name="setTeamMemberSecurityGroupIn${listSortTarget}Form" url="setTeamMemberSecurityGroupIn${listSortTarget}" partyId=team.partyId accountTeamPartyId=team.partyId teamMemberPartyId=member.partyId>
            <@inputSelectCell name="securityGroupId" list=salesTeamRoleSecurities key="securityGroupId" displayField="roleDescription" default=member.securityGroupId ignoreParameters=true />
            <@inputSubmitCell title=uiLabelMap.CommonUpdate />
          </@form>
        <#else>
          <@displayCell text="${(member.salesTeamRoleSecurity.description)!}" />
        </#if>
        <#if hasTeamRemovePermission!false>
          <@form name="removeTeamMemberIn${listSortTarget}Form" url="removeTeamMemberIn${listSortTarget}" partyId=team.partyId accountTeamPartyId=team.partyId teamMemberPartyId=member.partyId>
            <@inputSubmitCell title=uiLabelMap.CommonRemove />
          </@form>
        </#if>
      </tr>
    </#list>
  </table>
</#if>
