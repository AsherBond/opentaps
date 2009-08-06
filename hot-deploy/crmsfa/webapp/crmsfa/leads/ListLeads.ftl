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
  <div class="subSectionTitle">${uiLabelMap.CrmLeads}</div>
</div>
<div class="subSectionBlock">
    <@paginate name="listLeadsByPhone" list=crmPartyListBuilder rememberPage=false>
    <#noparse>
    <@navigationBar />
    <table class="crmsfaListTable">
        <tr class="crmsfaListTableHeader">
            <@headerCell title=uiLabelMap.CrmLeadName orderBy="partyId" blockClass="tableheadtext"/>
            <@headerCell title=uiLabelMap.CrmCompanyName orderBy="companyName" blockClass="tableheadtext"/>
            <@headerCell title=uiLabelMap.CommonCity orderBy="primaryCity" blockClass="tableheadtext"/>
            <@headerCell title=uiLabelMap.CrmPrimaryEmail orderBy="primaryEmail" blockClass="tableheadtext"/>
            <@headerCell title=uiLabelMap.PartyPhoneNumber orderBy="" blockClass="tableheadtext"/>                
            <#if pageRows?has_content && pageRows.get(0).hasAccountsRemoveAbility?default(false)><td>&nbsp;</td></#if>
        </tr>
        <#list pageRows as crmLead>
            <tr class="${tableRowClass(crmLead_index)}">
                <#if crmLead.sectionName?default("") == "lookup">
                    <@displayLinkCell href="javascript:set_value('${crmLead.partyId}')" text="${crmLead.groupName?if_exists} ${crmLead.firstName?if_exists} ${crmLead.lastName?if_exists} (${crmLead.partyId})"/>
                <#else>
                    <@displayLinkCell href="viewLead?partyId=${crmLead.partyId}" text="${crmLead.groupName?if_exists} ${crmLead.firstName?if_exists} ${crmLead.lastName?if_exists} (${crmLead.partyId})"/>
                </#if>
                <@displayCell text=crmLead.companyName?if_exists/>
                <@displayCell text=crmLead.abbrevPostalAddressByPurpose?if_exists/>                    
                <@displayCell text=crmLead.electronicAddressByPurpose/>
                <@displayCell text=crmLead.telecomNumberByPurpose/>
                <#if crmLead.hasContactRemoveAbility?default(false)>
                  <@displayLinkCell href="removeContactFromAccount/viewContact?partyId=${crmLead.parameters.partyId}&contactPartyId=${crmLead.parameters.partyId}&accountPartyId=${crmLead.partyId}" text=uiLabelMap.CommonRemove/>  
                </#if>            
            </tr>
        </#list>
    </table>
    </#noparse>
    </@paginate>
</div>

