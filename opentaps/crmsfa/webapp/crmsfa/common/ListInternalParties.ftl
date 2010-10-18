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
  <div class="subSectionTitle">${uiLabelMap.CrmClients}</div>
</div>
<#if searchByPhone?default(false)>
    <div class="subSectionBlock">
        <@paginate name="listClientsByPhone" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.PartyParty orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy="" blockClass="tableheadtext"/>                
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmClient>
                <tr class="${tableRowClass(crmClient_index)}">
                    <#if crmClient.groupName?has_content>
                      <#assign nameText = "${crmClient.groupName} (${crmClient.partyId})"/>
                    <#else>
                      <#assign nameText = "${crmClient.firstName?if_exists} ${crmClient.lastName?if_exists} (${crmClient.partyId})"/>
                    </#if>                
                    <#if crmClient.sectionName?default("") == "lookup">
                        <@displayLinkCell href="javascript:set_value('${crmClient.partyId}')" text=nameText blockClass="fieldWidth300"/>
                    <#else>
                        <@displayLinkCell href="viewContact?partyId=${crmClient.partyId}" text=nameText blockClass="fieldWidth300"/>
                    </#if>
                    <@displayCell text=crmClient.abbrevPostalAddressByPurpose blockClass="fieldWidth200"/>                    
                    <@displayCell text=formatTelecomNumber(crmClient.entityValue) blockClass="fieldWidth200"/>
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
<#else>
    <div class="subSectionBlock">
        <@paginate name="listClientsByName" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.PartyParty orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryPhone orderBy="" blockClass="tableheadtext"/>                
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmClient>
                <tr class="${tableRowClass(crmClient_index)}">
                    <#if crmClient.groupName?has_content>
                      <#assign nameText = "${crmClient.groupName} (${crmClient.partyId})"/>
                    <#else>
                      <#assign nameText = "${crmClient.firstName?if_exists} ${crmClient.lastName?if_exists} (${crmClient.partyId})"/>
                    </#if>
                    <#if crmClient.sectionName?default("") == "lookup">
                        <@displayLinkCell href="javascript:set_value('${crmClient.partyId}')" text=nameText blockClass="fieldWidth300"/>
                    <#else>
                        <@displayLinkCell href="viewContact?partyId=${crmClient.partyId}" text=nameText blockClass="fieldWidth300"/>
                    </#if>
                    <@displayCell text=crmClient.abbrevPostalAddressByPurpose blockClass="fieldWidth200"/>                    
                    <@displayCell text=crmClient.telecomNumberByPurpose blockClass="fieldWidth200"/>
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
</#if>
