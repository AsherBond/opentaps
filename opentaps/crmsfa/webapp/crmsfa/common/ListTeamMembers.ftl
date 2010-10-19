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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.CrmTeamMembers} </div>
</div>
<div class="subSectionBlock">
    <@paginate name="listTeamMembers" list=crmPartyListBuilder rememberPage=false isLookup=isLookup>
      <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.CommonName orderBy="firstName" blockClass="tableheadtext"/>
                <#if "true" != parameters.isLookup!"">
                  <@headerCell title=uiLabelMap.PartyRole orderBy="securityGroupId" blockClass="tableheadtext"/>
                  <@headerCell title=uiLabelMap.CommonUpdate orderBy="" blockClass="tableheadtext"/>
                  <@headerCell title=uiLabelMap.CommonRemove orderBy="" blockClass="tableheadtext"/>
                </#if>
            </tr>
            <#list pageRows as crmClient>
                <tr class="${tableRowClass(crmClient_index)}">
                <#assign roleDescription = ""/>
                <#if crmClient.securityGroupId?has_content>
                  <#assign roleDescription = delegator.findByPrimaryKey("SalesTeamRoleSecurity", Static["org.ofbiz.base.util.UtilMisc"].toMap("securityGroupId", crmClient.securityGroupId)).get("roleDescription") />
                </#if>
                <#if "true" == parameters.isLookup!"">
                  <@displayLinkCell href="javascript:set_value('${crmClient.partyId}')" text="${crmClient.firstName} ${crmClient.lastName} (${crmClient.partyId})" blockClass="fieldWidth300"/>
                <#else>
                  <@displayCell text="${crmClient.firstName} ${crmClient.lastName} (${crmClient.partyId})" blockClass="fieldWidth300"/>
                  <#if hasTeamUpdatePermission?exists>
                    <@inputSelectCell name="securityGroupId" key="securityGroupId" list=securityGroupsList displayField="roleDescription" required=false blockClass="fieldWidth300"/>
                  <#else>
                    <@displayCell text="${roleDescription}" blockClass="fieldWidth300"/>
                  </#if>
                  <#if hasTeamUpdatePermission!false>
                    <@inputConfirmCell title=uiLabelMap.CommonUpdate form="setTeamMemberSecurityGroupForm${crmClient_index}"/>
                  <#else>
                    <td/>
                  </#if>
                  <#if hasTeamRemovePermission!false>
                    <@inputConfirmCell title=uiLabelMap.CommonRemove form="removeTeamMemberSecurityGroupForm${crmClient_index}"/>
                  <#else>
                    <td/>
                  </#if>
                  <@form name="setTeamMemberSecurityGroupForm${crmClient_index}" url="setTeamMemberSecurityGroupIn${listSortTarget}" partyId="${parameters.partyId?if_exists}" accountTeamPartyId="${parameters.partyId?if_exists}" teamMemberPartyId="${crmClient.partyId}"/>
                  <@form name="removeTeamMemberSecurityGroupForm${crmClient_index}" url="removeTeamMemberIn${listSortTarget}" partyId="${parameters.partyId?if_exists}" accountTeamPartyId="${parameters.partyId?if_exists}" teamMemberPartyId="${crmClient.partyId}"/>
                </#if>

                </tr>
            </#list>
        </table>
      </#noparse>
    </@paginate>
</div>

