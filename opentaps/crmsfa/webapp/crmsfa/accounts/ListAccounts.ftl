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
    <div class="subSectionTitle">${uiLabelMap.CrmAccounts}</div>
</div>
<#if searchByPhone?default(false)>
    <div class="subSectionBlock">
        <@paginate name="listAccountsByPhone" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.CrmAccountName orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy="" blockClass="tableheadtext"/>
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmAccount>
                <tr class="${tableRowClass(crmAccount_index)}">
                    <#if crmAccount.sectionName?default("") == "lookup">
                        <@displayLinkCell href="javascript:set_value('${crmAccount.partyId}')" text="${crmAccount.groupName?if_exists} (${crmAccount.partyId})" blockClass="fieldWidth50pct"/>                      
                    <#else>
                        <@displayLinkCell href="viewAccount?partyId=${crmAccount.partyId}" text="${crmAccount.groupName?if_exists} (${crmAccount.partyId})" blockClass="fieldWidth50pct"/>                      
                    </#if>
                    <@displayCell text=crmAccount.abbrevPostalAddressByPurpose blockClass="fieldWidth25pct"/>
                    <@displayCell text=crmAccount.electronicAddressByPurpose blockClass="fieldWidth25pct"/>
                    <@displayCell text=formatTelecomNumber(crmAccount.entityValue) blockClass="fieldWidth25pct"/>
                    <#if crmAccount.hasAccountsRemoveAbility?default(false)>
                      <@displayLinkCell href="removeContactFromAccount/viewContact?partyId=${crmAccount.parameters.partyId}&contactPartyId=${crmAccount.parameters.partyId}&accountPartyId=${crmAccount.partyId}" text=uiLabelMap.CommonRemove/>  
                    </#if>            
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
<#else>
    <div class="subSectionBlock">
        <@paginate name="listAccountsByName" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.CrmAccountName orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryPhone orderBy="" blockClass="tableheadtext"/>
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmAccount>
                <tr class="${tableRowClass(crmAccount_index)}">
                    <#if crmAccount.sectionName?default("") == "lookup">
                        <@displayLinkCell href="javascript:set_value('${crmAccount.partyId}')" text="${crmAccount.groupName?if_exists} (${crmAccount.partyId})" blockClass="fieldWidth50pct"/>                        
                    <#else>
                        <@displayLinkCell href="viewAccount?partyId=${crmAccount.partyId}" text="${crmAccount.groupName?if_exists} (${crmAccount.partyId})" blockClass="fieldWidth50pct"/>                        
                    </#if>
                    <@displayCell text=crmAccount.abbrevPostalAddressByPurpose blockClass="fieldWidth25pct"/>
                    <@displayCell text=crmAccount.electronicAddressByPurpose blockClass="fieldWidth25pct"/>
                    <@displayCell text=crmAccount.telecomNumberByPurpose blockClass="fieldWidth25pct"/>
                    <#if crmAccount.hasAccountsRemoveAbility?default(false)>
                      <@displayLinkCell href="removeContactFromAccount/viewContact?partyId=${crmAccount.parameters.partyId}&contactPartyId=${crmAccount.parameters.partyId}&accountPartyId=${crmAccount.partyId}" text=uiLabelMap.CommonRemove/>  
                    </#if>            
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
</#if>
 

