<#--
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<div class="subSectionHeader">
    <div class="subSectionTitle">${uiLabelMap.CrmContacts}</div>
</div>
<#if searchByPhone?default(false)>
    <div class="subSectionBlock">
        <@paginate name="listContactsByPhone" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.CrmContactName orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy="" blockClass="tableheadtext"/>
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmContact>
                <tr class="${tableRowClass(crmContact_index)}">
	                <#if crmContact.sectionName?default("") == "lookup">
	                    <@displayLinkCell href="javascript:set_value('${crmContact.partyId}')" text="${crmContact.groupName?if_exists} ${crmContact.firstName?if_exists} ${crmContact.lastName?if_exists} (${crmContact.partyId})" blockClass="fieldWidth50pct"/>
	                <#else>
	                    <@displayLinkCell href="viewContact?partyId=${crmContact.partyId}" text="${crmContact.groupName?if_exists} ${crmContact.firstName?if_exists} ${crmContact.lastName?if_exists} (${crmContact.partyId})" blockClass="fieldWidth50pct"/>
	                </#if>
	                <@displayCell text=crmContact.abbrevPostalAddressByPurpose blockClass="fieldWidth25pct"/>
	                <@displayCell text=crmContact.electronicAddressByPurpose blockClass="fieldWidth25pct"/>
                    <@displayCell text=formatTelecomNumber(crmContact.entityValue) blockClass="fieldWidth25pct"/>
	                <#if crmContact.hasContactRemoveAbility?default(false)>
	                  <@displayLinkCell href="removeContactFromAccount/viewContact?partyId=${crmContact.parameters.partyId}&contactPartyId=${crmContact.parameters.partyId}&accountPartyId=${crmContact.partyId}" text=uiLabelMap.CommonRemove/>  
	                </#if>            
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
<#else>
    <div class="subSectionBlock">
        <@paginate name="listContactsByName" list=crmPartyListBuilder rememberPage=false>
        <#noparse>
        <@navigationBar />
        <table class="crmsfaListTable">
            <tr class="crmsfaListTableHeader">
                <@headerCell title=uiLabelMap.CrmContactName orderBy="partyId" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="primaryEmail" blockClass="tableheadtext"/>
                <@headerCell title=uiLabelMap.CrmPrimaryPhone orderBy="" blockClass="tableheadtext"/>
                <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
            </tr>
            <#list pageRows as crmContact>
                <tr class="${tableRowClass(crmContact_index)}">
                    <#if crmContact.sectionName?default("") == "lookup">
                        <@displayLinkCell href="javascript:set_value('${crmContact.partyId}')" text="${crmContact.groupName?if_exists} ${crmContact.firstName?if_exists} ${crmContact.lastName?if_exists} (${crmContact.partyId})"/>
                    <#else>
                        <@displayLinkCell href="viewContact?partyId=${crmContact.partyId}" text="${crmContact.groupName?if_exists} ${crmContact.firstName?if_exists} ${crmContact.lastName?if_exists} (${crmContact.partyId})"/>
                    </#if>
                    <@displayCell text=crmContact.abbrevPostalAddressByPurpose />
                    <@displayCell text=crmContact.electronicAddressByPurpose />
                    <@displayCell text=crmContact.telecomNumberByPurpose />
                    <#if crmContact.hasContactRemoveAbility?default(false)>
                      <@displayLinkCell href="removeContactFromAccount/viewContact?partyId=${crmContact.parameters.partyId}&contactPartyId=${crmContact.parameters.partyId}&accountPartyId=${crmContact.partyId}" text=uiLabelMap.CommonRemove/>  
                    </#if>            
                </tr>
            </#list>
        </table>
        </#noparse>
        </@paginate>
    </div>
</#if>

